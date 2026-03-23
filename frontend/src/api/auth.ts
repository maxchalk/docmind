import client from './client';
import { AuthResponse } from '../types';

export const login = async (email: string, password: string): Promise<AuthResponse> => {
  const response = await client.post<AuthResponse>('/auth/login', { email, password });
  return response.data;
};

export const register = async (email: string, password: string, role: string): Promise<AuthResponse> => {
  const response = await client.post<AuthResponse>('/auth/register', { email, password, role });
  return response.data;
};
