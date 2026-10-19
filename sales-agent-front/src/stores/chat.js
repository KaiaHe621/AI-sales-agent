import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { agentApi } from '@/api'
import { ElMessage } from 'element-plus'

const SESSION_KEY = 'chat-sessions'

function loadSessions() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || '[]')
  } catch {
    return []
  }
}

// 只保存结构数据，不在流式过程中调用
function saveSessions(sessions) {
  try {
    const toSave = sessions.map(s => ({
      id: s.id,
      title: s.title,
      createdAt: s.createdAt,
      messages: s.messages.map(m => ({
        id: m.id,
        role: m.role,
        content: m.content,
        chartOption: m.chartOption || null,
        status: m.status === 'streaming' ? 'done' : m.status,
      })),
    }))
    localStorage.setItem(SESSION_KEY, JSON.stringify(toSave))
  } catch {
    // localStorage 满了等情况静默处理
  }
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref(loadSessions())
  const activeSessionId = ref(sessions.value[0]?.id || null)
  const isStreaming = ref(false)
  let abortController = null

  const activeSession = computed(() =>
    sessions.value.find(s => s.id === activeSessionId.value) || null
  )

  function createSession() {
    const id = `session-${Date.now()}`
    const session = {
      id,
      title: '新对话',
      createdAt: new Date().toISOString(),
      messages: [],
    }
    sessions.value.unshift(session)
    activeSessionId.value = id
    saveSessions(sessions.value)
    return session
  }

  function deleteSession(sessionId) {
    agentApi.clearSession(sessionId).catch(() => {})
    const idx = sessions.value.findIndex(s => s.id === sessionId)
    if (idx !== -1) sessions.value.splice(idx, 1)
    if (activeSessionId.value === sessionId) {
      activeSessionId.value = sessions.value[0]?.id || null
    }
    saveSessions(sessions.value)
  }

  function switchSession(sessionId) {
    activeSessionId.value = sessionId
  }

  // 找到会话里最后一条 assistant 消息，直接修改属性（不写 localStorage）
  function patchLastAssistant(sessionId, patch) {
    const session = sessions.value.find(s => s.id === sessionId)
    if (!session) return
    const msgs = session.messages
    for (let i = msgs.length - 1; i >= 0; i--) {
      if (msgs[i].role === 'assistant') {
        const msg = msgs[i]
        if (patch.content !== undefined) msg.content = patch.content
        if (patch.chartOption !== undefined) msg.chartOption = patch.chartOption
        if (patch.status !== undefined) msg.status = patch.status
        break
      }
    }
  }

  async function sendMessage(text) {
    if (!text.trim() || isStreaming.value) return

    let session = activeSession.value
    if (!session) session = createSession()

    const sessionId = session.id
    const isFirstMsg = session.messages.length === 0

    // 添加用户消息
    session.messages.push({ id: Date.now(), role: 'user', content: text, status: 'done' })
    if (isFirstMsg) {
      session.title = text.slice(0, 20)
    }

    // 添加 assistant 占位消息
    session.messages.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: '',
      chartOption: null,
      status: 'streaming',
    })

    isStreaming.value = true
    abortController = new AbortController()

    let fullContent = ''
    let buffer = ''       // 跨 chunk 的行缓冲
    let chartResolved = false  // 图表 JSON 是否已成功解析
    let chartMarkerIdx = -1    // CHART_JSON: 在 fullContent 中的起始位置

    const CHART_MARKER = 'CHART_JSON:'

    try {
      const response = await fetch('/agent/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'sa-token': localStorage.getItem('sa-token') || '',
        },
        body: JSON.stringify({ sessionId, message: text }),
        signal: abortController.signal,
      })

      if (response.status === 401) {
        const { useAuthStore } = await import('@/stores/auth')
        useAuthStore().clearAuth()
        const { default: router } = await import('@/router')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''

        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const token = line.slice(5)
          if (token.trimEnd() === '[DONE]') continue

          fullContent += token

          // 图表已解析完成，后续 token 不再处理
          if (chartResolved) continue

          // 检测 CHART_JSON: 标记
          if (chartMarkerIdx === -1) {
            chartMarkerIdx = fullContent.indexOf(CHART_MARKER)
          }

          if (chartMarkerIdx !== -1) {
            // 尝试解析标记后面的 JSON（可能还在积累中）
            const jsonStr = fullContent.slice(chartMarkerIdx + CHART_MARKER.length).trim()
            try {
              const parsed = JSON.parse(jsonStr)
              chartResolved = true
              const textPart = fullContent.slice(0, chartMarkerIdx).trim() || '已为您生成图表，请查看下方：'
              patchLastAssistant(sessionId, { content: textPart, chartOption: parsed })
            } catch {
              // JSON 还未接收完整，只更新文字部分让用户看到进度
              const textPart = fullContent.slice(0, chartMarkerIdx).trim() || '正在生成图表数据...'
              patchLastAssistant(sessionId, { content: textPart })
            }
          } else {
            // 普通文本，直接更新
            patchLastAssistant(sessionId, { content: fullContent })
          }
        }
      }

      // 流结束后，若图表 JSON 仍未解析成功，做最后一次尝试
      if (!chartResolved && chartMarkerIdx !== -1) {
        const jsonStr = fullContent.slice(chartMarkerIdx + CHART_MARKER.length).trim()
        try {
          const parsed = JSON.parse(jsonStr)
          const textPart = fullContent.slice(0, chartMarkerIdx).trim() || '已为您生成图表，请查看下方：'
          patchLastAssistant(sessionId, { content: textPart, chartOption: parsed })
        } catch {
          // 最终也解析失败，把 CHART_JSON 前的文字展示出来
          patchLastAssistant(sessionId, { content: fullContent.slice(0, chartMarkerIdx).trim() })
        }
      }

      patchLastAssistant(sessionId, { status: 'done' })
    } catch (err) {
      if (err.name === 'AbortError') {
        patchLastAssistant(sessionId, { status: 'done' })
      } else {
        ElMessage.error('请求失败，请检查后端服务是否正常运行')
        patchLastAssistant(sessionId, {
          content: '请求失败，请检查后端服务是否正常运行。',
          status: 'error',
        })
      }
    } finally {
      isStreaming.value = false
      abortController = null
      // 流式结束后统一持久化一次
      saveSessions(sessions.value)
    }
  }

  function stopStreaming() {
    abortController?.abort()
  }

  return {
    sessions,
    activeSessionId,
    activeSession,
    isStreaming,
    createSession,
    deleteSession,
    switchSession,
    sendMessage,
    stopStreaming,
  }
})
