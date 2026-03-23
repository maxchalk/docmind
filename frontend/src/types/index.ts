export interface User {
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}

export interface Document {
  id: number;
  filename: string;
  originalName: string;
  fileType: string;
  fileSizeBytes: number;
  uploadedByEmail: string;
  uploadedAt: string;
  lastVerifiedDate: string | null;
  status: 'PROCESSING' | 'READY' | 'FAILED';
  chunkCount: number;
  errorMessage: string | null;
}

export interface DocumentStatus {
  status: 'PROCESSING' | 'READY' | 'FAILED';
  chunkCount: number;
}

export interface QuerySource {
  chunkIndex: number;
  pageNumber: number;
  snippet: string;
}

export interface QueryResponse {
  answer: string;
  sources: QuerySource[];
  stale: boolean;
  documentName: string;
}

export interface QueryHistoryItem {
  id: number;
  question: string;
  answer: string;
  sourcesJson: string;
  askedAt: string;
}

export interface ChatMessage {
  id: string;
  type: 'user' | 'ai';
  content: string;
  sources?: QuerySource[];
  isStale?: boolean;
  timestamp: Date;
  isLoading?: boolean;
}
