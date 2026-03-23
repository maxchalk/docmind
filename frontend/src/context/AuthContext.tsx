import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { User } from '../types';
import * as authApi from '../api/auth';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, role: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const savedToken = localStorage.getItem('docmind_token');
    const savedUser = localStorage.getItem('docmind_user');
    if (savedToken && savedUser) {
      setToken(savedToken);
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem('docmind_token');
        localStorage.removeItem('docmind_user');
      }
    }
  }, []);

  const login = async (email: string, password: string) => {
    const response = await authApi.login(email, password);
    const userData: User = { email: response.email, role: response.role };
    setToken(response.token);
    setUser(userData);
    localStorage.setItem('docmind_token', response.token);
    localStorage.setItem('docmind_user', JSON.stringify(userData));
  };

  const register = async (email: string, password: string, role: string) => {
    await authApi.register(email, password, role);
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('docmind_token');
    localStorage.removeItem('docmind_user');
  };

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
