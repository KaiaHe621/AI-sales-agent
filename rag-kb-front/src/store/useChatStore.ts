import { create } from 'zustand';
import type { ChatMessage } from '@/types';

interface ChatState {
  messages: ChatMessage[];
  sessionId: string | null;
  selectedKbIds: number[];
  isStreaming: boolean;
  setSessionId: (id: string | null) => void;
  setSelectedKbIds: (ids: number[]) => void;
  setMessages: (msgs: ChatMessage[]) => void;
  addMessage: (msg: ChatMessage) => void;
  updateLastAssistantMessage: (content: string) => void;
  setStreaming: (v: boolean) => void;
  clearMessages: () => void;
  setMessageFeedback: (id: string, feedback: number) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  sessionId: null,
  selectedKbIds: [],
  isStreaming: false,

  setSessionId: (id) => set({ sessionId: id }),
  setSelectedKbIds: (ids) => set({ selectedKbIds: ids }),
  setMessages: (msgs) => set({ messages: msgs }),
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),

  updateLastAssistantMessage: (content) =>
    set((s) => {
      const msgs = [...s.messages];
      for (let i = msgs.length - 1; i >= 0; i--) {
        if (msgs[i].role === 'assistant') {
          msgs[i] = { ...msgs[i], content };
          break;
        }
      }
      return { messages: msgs };
    }),

  setStreaming: (v) => set({ isStreaming: v }),

  clearMessages: () => set({ messages: [], sessionId: null }),

  setMessageFeedback: (id, feedback) =>
    set((s) => ({
      messages: s.messages.map((m) =>
        m.id === id ? { ...m, feedback } : m,
      ),
    })),
}));
