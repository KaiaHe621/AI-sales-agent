export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserInfo {
  userId: number;
  username: string;
  departmentId: string;
  role: string;
  token: string;
}

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string;
  departmentId: string;
  isPublic: boolean;
  createdBy: number;
  createdAt: string;
  updatedAt?: string;
  isDeleted?: boolean;
  permission?: 'READ' | 'WRITE' | 'ADMIN';
}

export interface KnowledgeBaseCreateRequest {
  name: string;
  description: string;
  departmentId: string;
  isPublic: boolean;
}

export interface KbDocument {
  id: number;
  kbId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  minioPath: string;
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  errorMsg: string | null;
  chunkCount: number;
  tokenCount: number;
  version: number;
  uploadedBy: number;
  uploadedAt: string;
  indexedAt: string | null;
  isDeleted: boolean;
}

export interface DocumentUploadResponse {
  docId: number;
  fileName: string;
  status: string;
  message: string;
}

export interface IndexStatusResponse {
  docId: number;
  fileName: string;
  status: string;
  errorMsg: string | null;
  chunkCount: number;
  tokenCount: number;
  indexedAt: string | null;
  retryCount: number;
}

export interface RagSource {
  chunkId: number;
  docId: number;
  docName?: string;
  pageNum: number | null;
  sectionTitle: string | null;
  excerpt: string;
  score: number;
}

export interface RagResponse {
  answer: string;
  sources: RagSource[];
  latencyMs: number;
  notFound: boolean;
}

export interface ChatRequest {
  question: string;
  kbIds: number[];
  sessionId?: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: RagSource[];
  latencyMs?: number;
  timestamp: number;
  feedback?: number;
}

export interface EvalReport {
  kbId: number;
  evalVersion: string;
  totalQuestions: number;
  hitCount: number;
  hitRate: number;
  mrr: number;
  avgFaithfulness: number;
  evalAt: string;
}

export interface TokenStats {
  embeddingTokens: number;
  contextTokens: number;
  generationTokens: number;
  totalTokens: number;
  estimatedCostCny: number;
}

export interface EvalDataset {
  id: number;
  kbId: number;
  question: string;
  expectedAnswer: string | null;
  expectedChunkIds: number[] | null;
  createdBy: number;
  createdAt: string;
}

export interface EvalDatasetRequest {
  question?: string;
  expectedAnswer?: string;
  expectedChunkIds?: number[];
}

export interface ChunkSummary {
  id: number;
  docId: number;
  chunkIndex: number;
  content: string;
  tokenCount: number;
}

export interface ChatSessionItem {
  id: string;
  userId: number;
  kbIds: string;
  title: string | null;
  messageCount: number;
  createdAt: string;
  lastActiveAt: string;
}

export interface ChatMessageItem {
  id: number;
  sessionId: string;
  role: string;
  content: string;
  sources: string | null;
  latencyMs: number | null;
  createdAt: string;
}
