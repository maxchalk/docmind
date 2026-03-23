import client from './client';
import { QueryResponse, QueryHistoryItem } from '../types';

export const askQuestion = async (documentId: number, question: string): Promise<QueryResponse> => {
  const response = await client.post<QueryResponse>('/query/ask', { documentId, question });
  return response.data;
};

export const getHistory = async (): Promise<QueryHistoryItem[]> => {
  const response = await client.get<QueryHistoryItem[]>('/query/history');
  return response.data;
};
