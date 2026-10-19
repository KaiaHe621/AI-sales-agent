import request from './request';
import type {
  ApiResponse,
  LoginRequest,
  KnowledgeBase,
  KnowledgeBaseCreateRequest,
  KbDocument,
  DocumentUploadResponse,
  IndexStatusResponse,
  RagResponse,
  EvalReport,
  EvalDataset,
  EvalDatasetRequest,
  ChunkSummary,
  TokenStats,
  ChatSessionItem,
  ChatMessageItem,
} from '@/types';

export const authApi = {
  login: (data: LoginRequest) =>
    request.post<ApiResponse<string>>('/auth/login', data),
  logout: () =>
    request.post<ApiResponse<void>>('/auth/logout'),
};

export const kbApi = {
  create: (data: KnowledgeBaseCreateRequest) =>
    request.post<ApiResponse<KnowledgeBase>>('/kb', data),
  list: () =>
    request.get<ApiResponse<KnowledgeBase[]>>('/kb'),
  uploadDocument: (kbId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return request.post<ApiResponse<DocumentUploadResponse>>(
      `/kb/${kbId}/documents`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
  },
  getDocuments: (kbId: number) =>
    request.get<ApiResponse<KbDocument[]>>(`/kb/${kbId}/documents`),
  getDocumentStatus: (kbId: number, docId: number) =>
    request.get<ApiResponse<IndexStatusResponse>>(`/kb/${kbId}/documents/${docId}/status`),
  deleteDocument: (kbId: number, docId: number) =>
    request.delete<ApiResponse<void>>(`/kb/${kbId}/documents/${docId}`),
  reindexDocument: (kbId: number, docId: number) =>
    request.post<ApiResponse<string>>(`/kb/${kbId}/documents/${docId}/reindex`),
  downloadDocument: (kbId: number, docId: number) =>
    request.get<Blob>(`/kb/${kbId}/documents/${docId}/download`, {
      responseType: 'blob',
    }),
};

export const chatApi = {
  syncQuery: (question: string, kbIds: number[], sessionId?: string) =>
    request.post<RagResponse>('/chat', { question, kbIds, sessionId }),
  createStreamUrl: (question: string, kbIds: number[], sessionId?: string) => {
    const params = new URLSearchParams();
    params.set('question', question);
    kbIds.forEach((id) => params.append('kbIds', String(id)));
    if (sessionId) params.set('sessionId', sessionId);
    return `/api/v1/chat/stream?${params.toString()}`;
  },
  listSessions: () =>
    request.get<ApiResponse<ChatSessionItem[]>>('/chat/sessions'),
  getMessages: (sessionId: string) =>
    request.get<ApiResponse<ChatMessageItem[]>>(`/chat/sessions/${sessionId}/messages`),
};

export const evalApi = {
  runEvaluation: (kbId: number, version: string) =>
    request.post<ApiResponse<EvalReport>>(`/eval/${kbId}/run`, null, {
      params: { version },
    }),
  getHistory: (kbId: number) =>
    request.get<ApiResponse<EvalReport[]>>(`/eval/${kbId}/history`),
  listDataset: (kbId: number) =>
    request.get<ApiResponse<EvalDataset[]>>(`/eval/${kbId}/dataset`),
  addQuestion: (kbId: number, data: EvalDatasetRequest) =>
    request.post<ApiResponse<EvalDataset>>(`/eval/${kbId}/dataset`, data),
  updateQuestion: (kbId: number, id: number, data: EvalDatasetRequest) =>
    request.put<ApiResponse<EvalDataset>>(`/eval/${kbId}/dataset/${id}`, data),
  deleteQuestion: (kbId: number, id: number) =>
    request.delete<ApiResponse<void>>(`/eval/${kbId}/dataset/${id}`),
  listChunks: (kbId: number) =>
    request.get<ApiResponse<ChunkSummary[]>>(`/eval/${kbId}/chunks`),
};

export const feedbackApi = {
  submit: (messageId: number, feedback: number, comment?: string) =>
    request.post<ApiResponse<void>>(`/feedback/${messageId}`, null, {
      params: { feedback, comment },
    }),
};

export const statsApi = {
  getTokenStats: () =>
    request.get<ApiResponse<TokenStats>>('/stats/tokens'),
};
