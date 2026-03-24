import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster, ToastBar } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import QueryPage from './pages/QueryPage';

const toastBase: React.CSSProperties = {
  background: 'rgba(15, 23, 42, 0.92)',
  color: '#e2e8f0',
  backdropFilter: 'blur(20px)',
  WebkitBackdropFilter: 'blur(20px)',
  borderRadius: '10px',
  fontSize: '13px',
  fontWeight: '500',
  padding: '9px 13px',
  maxWidth: '320px',
  boxShadow: '0 12px 40px rgba(0,0,0,0.5)',
};

const App: React.FC = () => {
  return (
    <Router>
      <AuthProvider>
        <Toaster
          position="top-center"
          containerStyle={{ top: '74px' }}
          gutter={6}
          toastOptions={{
            duration: 3200,
            success: { iconTheme: { primary: '#34d399', secondary: '#0f172a' } },
            error:   { iconTheme: { primary: '#f87171', secondary: '#0f172a' } },
          }}
        >
          {(t) => (
            <ToastBar
              toast={t}
              style={{
                ...toastBase,
                border: t.type === 'success'
                  ? '1px solid rgba(52, 211, 153, 0.35)'
                  : t.type === 'error'
                  ? '1px solid rgba(248, 113, 113, 0.35)'
                  : '1px solid rgba(99, 102, 241, 0.25)',
                // Drive enter/exit through opacity + translate ourselves
                transition: 'opacity 0.22s ease, transform 0.28s cubic-bezier(0.34,1.5,0.64,1)',
                opacity: t.visible ? 1 : 0,
                transform: t.visible ? 'translateY(0) scale(1)' : 'translateY(-10px) scale(0.96)',
              }}
            />
          )}
        </Toaster>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/query/:id"
            element={
              <ProtectedRoute>
                <QueryPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
};

export default App;
