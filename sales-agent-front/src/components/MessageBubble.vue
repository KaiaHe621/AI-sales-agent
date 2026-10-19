<template>
  <div :class="['msg-row', msg.role]">
    <!-- Assistant avatar -->
    <div v-if="msg.role === 'assistant'" class="avatar-wrap">
      <div class="avatar ai-avatar">
        <el-icon size="16"><Cpu /></el-icon>
      </div>
    </div>

    <div :class="['bubble-col', { 'has-chart': msg.chartOption }]">
      <div :class="['bubble', msg.role, { error: msg.status === 'error' }]">
        <!-- Streaming typing indicator -->
        <div v-if="msg.role === 'assistant' && msg.status === 'streaming' && !msg.content" class="typing-dots">
          <span></span><span></span><span></span>
        </div>

        <!-- Content -->
        <div
          v-if="msg.content"
          :class="msg.role === 'user' ? 'user-bubble' : ''"
        >
          <div
            v-if="msg.role === 'assistant'"
            class="md-content"
            v-html="renderedContent"
          ></div>
          <span v-else>{{ msg.content }}</span>
        </div>

        <!-- Streaming cursor -->
        <span
          v-if="msg.role === 'assistant' && msg.status === 'streaming' && msg.content"
          class="cursor"
        ></span>
      </div>

      <!-- Chart -->
      <ChartRenderer v-if="msg.chartOption" :option="msg.chartOption" />

      <!-- Actions (only for done assistant messages) -->
      <div v-if="msg.role === 'assistant' && msg.status === 'done' && msg.content" class="msg-actions">
        <button class="action-btn" @click="copyContent" :title="copied ? '已复制' : '复制'">
          <el-icon size="13"><DocumentCopy /></el-icon>
          {{ copied ? '已复制' : '复制' }}
        </button>
      </div>
    </div>

    <!-- User avatar -->
    <div v-if="msg.role === 'user'" class="avatar-wrap">
      <div class="avatar user-avatar">
        {{ userInitial }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Cpu, DocumentCopy } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import ChartRenderer from './ChartRenderer.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  msg: { type: Object, required: true },
})

const auth = useAuthStore()
const copied = ref(false)

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

const renderedContent = computed(() => md.render(props.msg.content || ''))

const userInitial = computed(() => {
  const name = auth.userInfo?.username || '我'
  return name.slice(-1)
})

async function copyContent() {
  try {
    await navigator.clipboard.writeText(props.msg.content)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {
    // fallback
  }
}
</script>

<style scoped>
.msg-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 4px 0;
  animation: msgIn 0.2s ease;
}

.msg-row.user {
  flex-direction: row-reverse;
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.avatar-wrap {
  flex-shrink: 0;
  padding-top: 2px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.ai-avatar {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.35);
}

.user-avatar {
  background: linear-gradient(135deg, #06B6D4, #0891B2);
  color: #fff;
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.35);
}

.bubble-col {
  max-width: 74%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.bubble-col.has-chart {
  max-width: 92%;
  width: 92%;
}

.msg-row.user .bubble-col {
  align-items: flex-end;
}

.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
  position: relative;
}

.bubble.assistant {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-top-left-radius: 4px;
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

.bubble.user {
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(79, 70, 229, 0.3);
}

.bubble.error {
  background: #FFF5F5;
  border-color: #FED7D7;
  color: #C53030;
}

/* Typing dots */
.typing-dots {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 4px 2px;
}
.typing-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: dot-bounce 1.3s ease infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.18s; }
.typing-dots span:nth-child(3) { animation-delay: 0.36s; }

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
  40%            { transform: scale(1);   opacity: 1;   }
}

/* Streaming cursor */
.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s step-end infinite;
}
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

/* Actions */
.msg-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.15s;
}
.msg-row:hover .msg-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  font-size: 12px;
  color: var(--text-muted);
  background: none;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.action-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
  background: var(--primary-light);
}
</style>
