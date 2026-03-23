import client from './client';
import { Document, DocumentStatus } from '../types';

export const getDocuments = async (): Promise<Document[]> => {
  const response = await client.get<Document[]>('/documents');
  return response.data;
};

export const uploadDocument = async (file: File): Promise<Document> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await client.post<Document>('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const deleteDocument = async (id: number): Promise<void> => {
  await client.delete(`/documents/${id}`);
};

export const getDocumentStatus = async (id: number): Promise<DocumentStatus> => {
  const response = await client.get<DocumentStatus>(`/documents/${id}/status`);
  return response.data;
};

export const verifyDocument = async (id: number): Promise<Document> => {
  const response = await client.patch<Document>(`/documents/${id}/verify`);
  return response.data;
};
