import { useState, useRef, useEffect, useCallback } from 'react';
import { Input, Button, Select, Tag, Tooltip, Space, Empty, App, Spin } from 'antd';
import {
  SendOutlined,
  ClearOutlined,
  LikeOutlined,
  DislikeOutlined,
  LikeFilled,
  DislikeFilled,
  LoadingOutlined,
  LinkOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  RobotOutlined,
  UserOutlined,
  PlusOutlined,
  HistoryOutlined,
  MessageOutlined,
} from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { kbApi, chatApi } from '@/api';
import { useChatStore } from '@/store/useChatStore';
import type { KnowledgeBase, ChatMessage, RagSource, ChatSessionItem } from '@/types';

export default function ChatPage() {
  const [kbList, setKbList] = useState<KnowledgeBase[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [sessions, setSessions] = useState<ChatSessionItem[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { message: antMsg } = App.useApp();
  const eventSourceRef = useRef<EventSource | null>(null);

  const {
    messages, sessionId, selectedKbIds, isStreaming,
    setSessionId, setSelectedKbIds, setMessages,
    addMessage, updateLastAssistantMessage,
    setStreaming, clearMessages, setMessageFeedback,
  } = useChatStore();

  const fetchSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      const res = await chatApi.listSessions();
      if (res.data.code === 200) setSessions(res.data.data);
    } catch {}
    setSessionsLoading(false);
  }, []);

  useEffect(() => {
    kbApi.list().then((res) => {
      if (res.data.code === 200) setKbList(res.data.data);
    }).catch(() => {});
    fetchSessions();
  }, [fetchSessions]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadSession = useCallback(async (session: ChatSessionItem) => {
    if (isStreaming) return;
    setHistoryLoading(true);
    try {
      const res = await chatApi.getMessages(session.id);
      if (res.data.code === 200) {
        const converted: ChatMessage[] = res.data.data.map((m) => {
          let sources: RagSource[] | undefined;
          if (m.sources) {
            try { sources = JSON.parse(m.sources); } catch {}
          }
          return {
            id: `hist-${m.id}`,
            role: m.role === 'USER' ? 'user' as const : 'assistant' as const,
            content: m.content,
            sources,
            latencyMs: m.latencyMs ?? undefined,
            timestamp: new Date(m.createdAt).getTime(),
          };
        });
        setMessages(converted);
        setSessionId(session.id);
      }
    } catch {
      antMsg.error('加载历史消息失败');
    }
    setHistoryLoading(false);
  }, [isStreaming, setMessages, setSessionId, antMsg]);

  const handleNewChat = useCallback(() => {
    if (isStreaming) return;
    clearMessages();
    fetchSessions();
  }, [isStreaming, clearMessages, fetchSessions]);

  const handleSend = useCallback(async () => {
    const question = inputValue.trim();
    if (!question || isStreaming) return;
    if (selectedKbIds.length === 0) {
      antMsg.warning('请先选择至少一个知识库');
      return;
    }

    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: Date.now(),
    };
    addMessage(userMsg);
    setInputValue('');

    const assistantMsg: ChatMessage = {
      id: `ai-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    };
    addMessage(assistantMsg);
    setStreaming(true);

    const params = new URLSearchParams();
    params.set('question', question);
    selectedKbIds.forEach((id) => params.append('kbIds', String(id)));
    if (sessionId) params.set('sessionId', sessionId);

    const token = localStorage.getItem('token');
    const url = `/api/v1/chat/stream?${params.toString()}`;
    const abortController = new AbortController();
    eventSourceRef.current = { close: () => abortController.abort() } as any;
    let fullContent = '';

    try {
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
        signal: abortController.signal,
      });

      if (!response.ok || !response.body) {
        updateLastAssistantMessage('请求失败，请检查登录状态后重试。');
        setStreaming(false);
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      let sseEventType = '';
      let sseDataLines: string[] = [];

      const dispatchSseEvent = () => {
        if (sseDataLines.length === 0) return;
        const data = sseDataLines.join('\n');
        const event = sseEventType || 'message';
        sseEventType = '';
        sseDataLines = [];

        if (event === 'status') {
          try {
            const parsed = JSON.parse(data);
            if (parsed.type === 'RETRIEVING') {
              updateLastAssistantMessage('正在检索知识库...');
            } else if (parsed.type === 'GENERATING') {
              updateLastAssistantMessage('');
            }
          } catch {}
        } else if (event === 'token') {
          fullContent += data;
          updateLastAssistantMessage(fullContent);
        } else if (event === 'done') {
          try {
            const parsed = JSON.parse(data);
            const sources: RagSource[] = parsed.sources || [];

            let displayContent = fullContent;
            sources.forEach((s, i) => {
              const ref = `[参考${i + 1}]`;
              const name = s.sectionTitle || s.docName || `来源${i + 1}`;
              displayContent = displayContent.replaceAll(ref, `[${name}]`);
            });

            const lastMsgs = useChatStore.getState().messages;
            const updated = [...lastMsgs];
            for (let i = updated.length - 1; i >= 0; i--) {
              if (updated[i].role === 'assistant') {
                updated[i] = { ...updated[i], content: displayContent, sources, latencyMs: parsed.latencyMs };
                break;
              }
            }
            useChatStore.setState({ messages: updated });
          } catch (e) {
            console.error('[Chat] done event parse error:', e, 'raw data:', data);
          }
          setStreaming(false);
        } else if (event === 'error') {
          if (!fullContent) updateLastAssistantMessage('生成过程中出现异常，请稍后重试。');
          setStreaming(false);
        }
      };

      let pending = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value, { stream: true });
        pending += text;
        const lines = pending.split('\n');
        pending = lines.pop() || '';
        for (const line of lines) {
          const stripped = line.replace(/\r$/, '');
          if (stripped === '') {
            dispatchSseEvent();
          } else if (stripped.startsWith('event:')) {
            sseEventType = stripped.slice(6).trim();
          } else if (stripped.startsWith('data:')) {
            sseDataLines.push(stripped.slice(5));
          }
        }
      }
      if (sseDataLines.length > 0) dispatchSseEvent();
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        if (!fullContent) updateLastAssistantMessage('连接异常，请稍后重试。');
      }
    }
    setStreaming(false);

    const finalMsgs = useChatStore.getState().messages;
    const finalUpdated = [...finalMsgs];
    for (let i = finalUpdated.length - 1; i >= 0; i--) {
      if (finalUpdated[i].role === 'assistant' && finalUpdated[i].sources === undefined) {
        finalUpdated[i] = { ...finalUpdated[i], sources: [] };
        useChatStore.setState({ messages: finalUpdated });
        break;
      }
    }

    fetchSessions();
  }, [inputValue, isStreaming, selectedKbIds, sessionId, addMessage, updateLastAssistantMessage, setStreaming, antMsg, fetchSessions]);

  const handleFeedback = async (msgId: string, feedback: number) => {
    setMessageFeedback(msgId, feedback);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="h-full flex gap-4">
      {/* Left sidebar - session list */}
      <div className="w-64 flex-shrink-0 flex flex-col glass-card p-0 overflow-hidden">
        <div className="p-3 border-b border-gray-100">
          <span className="text-sm font-semibold flex items-center gap-1.5">
            <HistoryOutlined style={{ color: '#6366f1' }} /> 历史会话
          </span>
        </div>
        <div className="flex-1 overflow-auto p-2 space-y-1">
          {sessionsLoading ? (
            <div className="flex justify-center py-8"><Spin size="small" /></div>
          ) : sessions.length === 0 ? (
            <div className="text-center text-xs text-gray-400 py-8">暂无历史会话</div>
          ) : (
            sessions.map((s) => (
              <div
                key={s.id}
                onClick={() => loadSession(s)}
                className={`px-3 py-2.5 rounded-lg cursor-pointer transition-all text-sm truncate ${
                  sessionId === s.id
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'hover:bg-gray-50 text-gray-600'
                }`}
              >
                <div className="flex items-center gap-1.5">
                  <MessageOutlined className="text-xs flex-shrink-0" />
                  <span className="truncate">{s.title || '新对话'}</span>
                </div>
                <div className="text-[10px] text-gray-400 mt-1 pl-4">
                  {new Date(s.lastActiveAt).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}
                  {' · '}{s.messageCount} 条
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Right main chat area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* KB selector header */}
        <div className="glass-card p-4 mb-4 flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <DatabaseOutlined style={{ color: '#4f46e5' }} />
            <span className="text-sm font-medium">选择知识库</span>
          </div>
          <Select
            mode="multiple"
            value={selectedKbIds}
            onChange={setSelectedKbIds}
            placeholder="选择要查询的知识库..."
            className="flex-1 min-w-[200px]"
            options={kbList.map((kb) => ({
              label: (
                <span className="flex items-center gap-2">
                  {kb.name}
                  {kb.isPublic && <Tag color="green" className="!text-xs !m-0">公开</Tag>}
                </span>
              ),
              value: kb.id,
            }))}
            maxTagCount={3}
          />
          <Tooltip title="新对话">
            <Button
              icon={<PlusOutlined />}
              onClick={handleNewChat}
              className="!border-gray-200"
            >
              新对话
            </Button>
          </Tooltip>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-auto pr-2 space-y-4 mb-4">
          {historyLoading ? (
            <div className="h-full flex items-center justify-center"><Spin size="large" /></div>
          ) : messages.length === 0 ? (
            <div className="h-full flex items-center justify-center">
              <div className="text-center animate-fade-in">
                <RobotOutlined
                  className="text-6xl mb-6 animate-pulse-slow"
                  style={{ color: '#4f46e5' }}
                />
                <h2 className="text-2xl font-bold gradient-text mb-3">
                  开始智能问答
                </h2>
                <p className="text-dark-muted max-w-md mx-auto">
                  选择知识库，输入您的问题，AI 会基于知识库内容给出精准回答并标注引用来源。
                </p>
                <div className="flex flex-wrap gap-2 justify-center mt-6">
                  {['新员工入职流程是什么？', 'API 限流策略是什么？', '年假是怎么规定的？'].map((q) => (
                    <Button
                      key={q}
                      size="small"
                      className="!border-gray-200 !text-dark-muted hover:!text-primary-400 hover:!border-primary-500"
                      onClick={() => setInputValue(q)}
                    >
                      {q}
                    </Button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                onFeedback={handleFeedback}
              />
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input area */}
        <div className="glass-card p-4">
          <div className="flex gap-3 items-end">
            <Input.TextArea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入您的问题... (Enter 发送, Shift+Enter 换行)"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={isStreaming}
              className="!bg-gray-50 !border-gray-200"
            />
            <Button
              type="primary"
              icon={isStreaming ? <LoadingOutlined /> : <SendOutlined />}
              onClick={handleSend}
              disabled={isStreaming || !inputValue.trim()}
              className="h-10 px-6 !bg-gradient-to-r !from-blue-500 !to-cyan-500 !border-none hover:!from-blue-600 hover:!to-cyan-600"
            >
              {isStreaming ? '生成中' : '发送'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function MessageBubble({
  message: msg,
  onFeedback,
}: {
  message: ChatMessage;
  onFeedback: (id: string, feedback: number) => void;
}) {
  const isUser = msg.role === 'user';

  return (
    <div className={`flex gap-3 animate-slide-up ${isUser ? 'flex-row-reverse' : ''}`}>
      <div
        className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${
          isUser
            ? 'bg-gradient-to-br from-primary-500 to-purple-600'
            : 'bg-gradient-to-br from-cyan-600 to-primary-600'
        }`}
      >
        {isUser ? (
          <UserOutlined className="text-white text-sm" />
        ) : (
          <RobotOutlined className="text-white text-sm" />
        )}
      </div>

      <div className={`max-w-[75%] ${isUser ? 'text-right' : ''}`}>
        <div className={isUser ? 'chat-bubble-user px-4 py-3' : 'chat-bubble-ai px-4 py-3'}>
          {msg.content ? (
            <div className="markdown-body text-sm">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                rehypePlugins={[rehypeRaw]}
                components={{
                  code({ className, children, ...props }) {
                    const match = /language-(\w+)/.exec(className || '');
                    const inline = !match;
                    return inline ? (
                      <code className={className} {...props}>{children}</code>
                    ) : (
                      <SyntaxHighlighter
                        style={oneDark}
                        language={match[1]}
                        PreTag="div"
                      >
                        {String(children).replace(/\n$/, '')}
                      </SyntaxHighlighter>
                    );
                  },
                }}
              >
                {msg.content}
              </ReactMarkdown>
              {!isUser && msg.sources === undefined && (
                <span className="inline-block w-1.5 h-4 bg-primary-500 animate-pulse ml-0.5 align-text-bottom rounded-sm" />
              )}
            </div>
          ) : (
            <div className="typing-indicator py-1">
              <span /><span /><span />
            </div>
          )}
        </div>

        {/* Sources & Feedback */}
        {!isUser && msg.sources && msg.sources.length > 0 && (
          <div className="mt-2 space-y-2">
            <div className="flex flex-wrap gap-1.5">
              {msg.sources.map((s, i) => (
                <Tooltip
                  key={i}
                  title={
                    <div className="max-w-xs">
                      <div className="font-medium mb-1">{s.sectionTitle || '相关段落'}</div>
                      <div className="text-xs opacity-80">{s.excerpt?.slice(0, 150)}...</div>
                      <div className="text-xs mt-1 opacity-60">相关度: {(s.score * 100).toFixed(1)}%</div>
                    </div>
                  }
                >
                  <Tag
                    icon={<LinkOutlined />}
                    className="!border-primary-200 !bg-primary-50 !text-primary-700 cursor-pointer !text-xs"
                  >
                    {s.sectionTitle || `来源${i + 1}`}
                  </Tag>
                </Tooltip>
              ))}
            </div>
            {msg.latencyMs !== undefined && (
              <div className="text-xs text-dark-muted flex items-center gap-1">
                <ClockCircleOutlined className="text-[10px]" /> {msg.latencyMs}ms
              </div>
            )}
          </div>
        )}

        {!isUser && msg.content && (
          <Space className="mt-2">
            <Tooltip title="有用">
              <Button
                type="text"
                size="small"
                icon={msg.feedback === 1 ? <LikeFilled className="!text-green-400" /> : <LikeOutlined />}
                onClick={() => onFeedback(msg.id, 1)}
                className="!text-dark-muted hover:!text-green-400 !text-xs"
              />
            </Tooltip>
            <Tooltip title="不好">
              <Button
                type="text"
                size="small"
                icon={msg.feedback === -1 ? <DislikeFilled className="!text-red-400" /> : <DislikeOutlined />}
                onClick={() => onFeedback(msg.id, -1)}
                className="!text-dark-muted hover:!text-red-400 !text-xs"
              />
            </Tooltip>
          </Space>
        )}
      </div>
    </div>
  );
}
