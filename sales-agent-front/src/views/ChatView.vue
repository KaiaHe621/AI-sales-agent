<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <aside :class="['sidebar', { collapsed: sidebarCollapsed }]">
      <!-- Brand -->
      <div class="sidebar-brand">
        <div class="brand-logo">
          <el-icon size="18" color="#fff"><TrendCharts /></el-icon>
        </div>
        <transition name="slide-fade">
          <span v-if="!sidebarCollapsed" class="brand-text">智能销售分析</span>
        </transition>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon size="14">
            <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </button>
      </div>

      <!-- New Chat Button -->
      <div class="sidebar-new">
        <button class="new-chat-btn" @click="newChat" :title="sidebarCollapsed ? '新对话' : ''">
          <el-icon size="15"><Plus /></el-icon>
          <span v-if="!sidebarCollapsed">新对话</span>
        </button>
      </div>

      <!-- Session List -->
      <div class="session-list" v-if="!sidebarCollapsed">
        <p class="list-label">历史对话</p>
        <div
          v-for="session in chat.sessions"
          :key="session.id"
          :class="['session-item', { active: session.id === chat.activeSessionId }]"
          @click="chat.switchSession(session.id)"
        >
          <el-icon size="13" class="session-icon"><ChatLineRound /></el-icon>
          <span class="session-title">{{ session.title }}</span>
          <button
            class="del-btn"
            @click.stop="confirmDelete(session.id)"
            title="删除"
          >
            <el-icon size="12"><Delete /></el-icon>
          </button>
        </div>
        <div v-if="chat.sessions.length === 0" class="empty-sessions">
          暂无对话记录
        </div>
      </div>

      <!-- Quick questions (only expanded) -->
      <div class="quick-section" v-if="!sidebarCollapsed">
        <p class="list-label">快捷提问</p>
        <div
          v-for="q in quickQuestions"
          :key="q.text"
          class="quick-item"
          @click="sendQuick(q.text)"
        >
          <span class="quick-emoji">{{ q.icon }}</span>
          <span class="quick-text">{{ q.text }}</span>
        </div>
      </div>

      <!-- User Info -->
      <div :class="['sidebar-user', { collapsed: sidebarCollapsed }]">
        <div class="user-avatar">{{ userInitial }}</div>
        <div v-if="!sidebarCollapsed" class="user-meta">
          <p class="user-name">{{ auth.userInfo?.username }}</p>
          <p class="user-role">{{ roleLabel }}</p>
        </div>
        <button v-if="!sidebarCollapsed" class="logout-btn" @click="auth.logout()" title="退出">
          <el-icon size="14"><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <!-- Main Chat Area -->
    <main class="chat-main">
      <!-- Header -->
      <header class="chat-header">
        <div class="header-left">
          <div class="header-icon">
            <el-icon size="16" color="#4F46E5"><Cpu /></el-icon>
          </div>
          <div>
            <h2 class="header-title">销售数据分析助手</h2>
            <p class="header-sub">基于 LangChain4j</p>
          </div>
        </div>
        <div class="header-right">
          <el-tag v-if="chat.isStreaming" type="success" size="small" class="streaming-tag">
            <el-icon class="spinning"><Loading /></el-icon>
            AI 思考中
          </el-tag>
          <el-button
            v-if="chat.activeSession"
            size="small"
            plain
            :icon="Delete"
            @click="clearCurrentSession"
            class="clear-btn"
          >清空记忆</el-button>
        </div>
      </header>

      <!-- Messages -->
      <div class="messages-container" ref="messagesEl">
        <!-- Welcome screen -->
        <div v-if="!chat.activeSession || chat.activeSession.messages.length === 0" class="welcome">
          <div class="welcome-icon">
            <el-icon size="36" color="#4F46E5"><TrendCharts /></el-icon>
          </div>
          <h3 class="welcome-title">你好，{{ auth.userInfo?.username }} 👋</h3>
          <p class="welcome-desc">我是你的专属 AI 销售数据分析助手，可以帮你查询销售数据、生成图表、发现异常。</p>
          <div class="welcome-cards">
            <div
              v-for="card in welcomeCards"
              :key="card.text"
              class="welcome-card"
              @click="sendQuick(card.text)"
            >
              <div class="wc-icon">{{ card.icon }}</div>
              <div class="wc-text">{{ card.text }}</div>
            </div>
          </div>
        </div>

        <!-- Message list -->
        <div v-else class="message-list">
          <MessageBubble
            v-for="msg in chat.activeSession.messages"
            :key="msg.id"
            :msg="msg"
          />
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-row">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="问我关于销售数据的任何问题… (Enter 发送，Shift+Enter 换行)"
            resize="none"
            class="chat-input"
            :disabled="chat.isStreaming"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="send-btns">
            <el-button
              v-if="chat.isStreaming"
              type="danger"
              :icon="VideoPause"
              circle
              @click="chat.stopStreaming()"
              title="停止生成"
              class="stop-btn"
            />
            <el-button
              v-else
              type="primary"
              :icon="Promotion"
              circle
              :disabled="!inputText.trim()"
              @click="handleSend"
              class="send-btn-circle"
            />
          </div>
        </div>
        <p class="input-hint">AI 可能产生错误，重要决策请以实际数据为准</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import {
  TrendCharts, Plus, ChatLineRound, Delete, SwitchButton,
  Cpu, Promotion, VideoPause, Loading, Fold, Expand,
} from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { agentApi } from '@/api'
import MessageBubble from '@/components/MessageBubble.vue'

const auth = useAuthStore()
const chat = useChatStore()

const inputText = ref('')
const messagesEl = ref(null)
const sidebarCollapsed = ref(false)

const userInitial = computed(() => {
  const name = auth.userInfo?.username || '我'
  return name.slice(-1)
})

const roleLabel = computed(() => {
  const role = auth.userInfo?.role
  const map = { DIRECTOR: '总监', MANAGER: '大区经理', REP: '销售代表' }
  return map[role] || role || '销售代表'
})

const quickQuestions = [
  { icon: '📈', text: '近6个月销售趋势' },
  { icon: '🏆', text: '本季度Top5销售员' },
  { icon: '🗺️', text: '各大区销售排名' },
  { icon: '⚠️', text: '有没有数据异常' },
  { icon: '📦', text: '最畅销产品Top10' },
  { icon: '💰', text: '2024年各月总销售额' },
]

const welcomeCards = [
  { icon: '📊', text: '近6个月销售趋势，生成折线图' },
  { icon: '🏆', text: '第四季度各大区销售排名' },
  { icon: '⚠️', text: '最近销售数据有没有异常' },
  { icon: '💡', text: '哪个产品品类利润最高' },
]

function scrollToBottom(smooth = true) {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTo({
        top: messagesEl.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'instant',
      })
    }
  })
}

watch(
  () => chat.activeSession?.messages?.length,
  () => scrollToBottom(),
)

watch(
  () => {
    const msgs = chat.activeSession?.messages
    if (!msgs?.length) return ''
    const last = msgs[msgs.length - 1]
    return last?.role === 'assistant' ? last.content : ''
  },
  () => scrollToBottom(false),
)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || chat.isStreaming) return
  inputText.value = ''
  if (!chat.activeSession) chat.createSession()
  await chat.sendMessage(text)
}

function sendQuick(text) {
  if (chat.isStreaming) return
  if (!chat.activeSession) chat.createSession()
  chat.sendMessage(text)
}

function newChat() {
  chat.createSession()
}

function confirmDelete(sessionId) {
  ElMessageBox.confirm('确认删除这条对话记录？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger',
  }).then(() => {
    chat.deleteSession(sessionId)
  }).catch(() => {})
}

async function clearCurrentSession() {
  if (!chat.activeSession) return
  await ElMessageBox.confirm('清空后 AI 将忘记本次对话上下文，是否继续？', '清空记忆', {
    confirmButtonText: '确认清空',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => null)

  const sessionId = chat.activeSession.id
  try {
    await agentApi.clearSession(sessionId)
    ElMessage.success('对话记忆已清空')
  } catch {
    ElMessage.error('清空失败')
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-page);
}

/* ===== SIDEBAR ===== */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease, min-width 0.25s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.brand-logo {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex: 1;
  white-space: nowrap;
}

.collapse-btn {
  margin-left: auto;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.collapse-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.sidebar-new {
  padding: 12px 12px 6px;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 12px;
  background: rgba(79, 70, 229, 0.2);
  border: 1px solid rgba(79, 70, 229, 0.35);
  border-radius: 9px;
  color: #A5B4FC;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.new-chat-btn:hover {
  background: rgba(79, 70, 229, 0.35);
  color: #fff;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.list-label {
  font-size: 11px;
  color: rgba(255,255,255,0.25);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 10px 6px 4px;
  font-weight: 600;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  color: rgba(255,255,255,0.55);
  font-size: 13px;
  position: relative;
}
.session-item:hover {
  background: var(--sidebar-hover);
  color: rgba(255,255,255,0.85);
}
.session-item.active {
  background: var(--sidebar-active);
  color: #fff;
  border: 1px solid rgba(79,70,229,0.3);
}

.session-icon { flex-shrink: 0; }

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.del-btn {
  display: none;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 4px;
  color: rgba(255,255,255,0.35);
  cursor: pointer;
  transition: all 0.15s;
  padding: 0;
  flex-shrink: 0;
}
.session-item:hover .del-btn {
  display: flex;
}
.del-btn:hover {
  background: rgba(239,68,68,0.2);
  color: #F87171;
}

.empty-sessions {
  font-size: 12px;
  color: rgba(255,255,255,0.2);
  text-align: center;
  padding: 20px 0;
}

.quick-section {
  padding: 0 8px;
  border-top: 1px solid rgba(255,255,255,0.06);
  padding-top: 4px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 7px;
  cursor: pointer;
  transition: background 0.15s;
  color: rgba(255,255,255,0.45);
  font-size: 12px;
}
.quick-item:hover {
  background: var(--sidebar-hover);
  color: rgba(255,255,255,0.75);
}
.quick-emoji { font-size: 14px; flex-shrink: 0; }
.quick-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.sidebar-user.collapsed {
  justify-content: center;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #06B6D4, #0891B2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-meta {
  flex: 1;
  overflow: hidden;
}
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-role {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
}

.logout-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 7px;
  color: rgba(255,255,255,0.35);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.logout-btn:hover {
  background: rgba(239,68,68,0.15);
  border-color: rgba(239,68,68,0.3);
  color: #F87171;
}

/* ===== MAIN ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 38px;
  height: 38px;
  background: var(--primary-light);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}
.header-sub {
  font-size: 11px;
  color: var(--text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.streaming-tag {
  animation: pulse 1.5s ease infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.6; }
}

.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.clear-btn {
  font-size: 12px;
}

/* ===== MESSAGES ===== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 860px;
  margin: 0 auto;
}

/* Welcome */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 72px;
  height: 72px;
  background: var(--primary-light);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(79, 70, 229, 0.15);
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.welcome-desc {
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 500px;
  line-height: 1.7;
  margin-bottom: 32px;
}

.welcome-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 560px;
  width: 100%;
}

.welcome-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}
.welcome-card:hover {
  border-color: var(--primary);
  background: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.wc-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.wc-text {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  line-height: 1.4;
}

.welcome-card:hover .wc-text {
  color: var(--primary);
}

/* ===== INPUT ===== */
.input-area {
  padding: 16px 24px 20px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 860px;
  margin: 0 auto;
}

:deep(.chat-input .el-textarea__inner) {
  border-radius: 12px;
  border-color: var(--border);
  padding: 12px 16px;
  font-size: 14px;
  resize: none;
  font-family: var(--font-sans);
  line-height: 1.6;
  transition: border-color 0.15s, box-shadow 0.15s;
}
:deep(.chat-input .el-textarea__inner:focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.send-btns {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.send-btn-circle,
.stop-btn {
  width: 40px;
  height: 40px;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 8px;
  max-width: 860px;
  margin-left: auto;
  margin-right: auto;
}

/* Slide fade transition */
.slide-fade-enter-active {
  transition: all 0.2s ease;
}
.slide-fade-leave-active {
  transition: all 0.15s ease;
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
