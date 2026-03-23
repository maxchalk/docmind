import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Brain, Mail, Lock, Loader2, UserPlus, Shield } from 'lucide-react';
import toast from 'react-hot-toast';

const RegisterPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('EMPLOYEE');
  const [isLoading, setIsLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password.length < 6) {
      toast.error('Password must be at least 6 characters');
      return;
    }
    setIsLoading(true);
    try {
      await register(email, password, role);
      toast.success('Account created! Please sign in.');
      navigate('/login');
    } catch (error: any) {
      const message = error.response?.data?.error || 'Registration failed. Please try again.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      {/* Background effects */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 -left-32 w-96 h-96 bg-brand-500/10 rounded-full blur-3xl animate-pulse-slow" />
        <div className="absolute bottom-1/4 -right-32 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-pulse-slow" />
      </div>

      <div className="glass-card w-full max-w-md p-8 animate-fade-in relative z-10">
        {/* Logo */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-brand-400 to-purple-500 flex items-center justify-center shadow-lg shadow-brand-500/30">
            <Brain className="w-7 h-7 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold gradient-text">DocMind</h1>
            <p className="text-xs text-surface-400 -mt-0.5">AI Document Intelligence</p>
          </div>
        </div>

        <h2 className="text-xl font-semibold text-white text-center mb-1">Create Account</h2>
        <p className="text-surface-400 text-sm text-center mb-8">Join the platform to start analyzing documents</p>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-surface-300 mb-2">Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                className="input-field pl-10"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-300 mb-2">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Min. 6 characters"
                className="input-field pl-10"
                required
                minLength={6}
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-surface-300 mb-2">Role</label>
            <div className="relative">
              <Shield className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="input-field pl-10 appearance-none cursor-pointer"
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="HR">HR</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          </div>

          <button
            type="submit"
            className="btn-primary w-full flex items-center justify-center gap-2"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Creating account...
              </>
            ) : (
              <>
                <UserPlus className="w-4 h-4" />
                Create Account
              </>
            )}
          </button>
        </form>

        <p className="text-center text-surface-400 text-sm mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-brand-400 hover:text-brand-300 font-medium transition-colors">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
