import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import StatusBadge from '../components/StatusBadge';
import UploadModal from '../components/UploadModal';
import { getDocuments, deleteDocument, getDocumentStatus, verifyDocument } from '../api/documents';
import { Document } from '../types';
import toast from 'react-hot-toast';
import {
  Brain, LogOut, Plus, FileText, Trash2, MessageSquare,
  AlertTriangle, CheckCircle, Files, Cpu, Clock, Shield, Search
} from 'lucide-react';

const DashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchDocuments = useCallback(async () => {
    try {
      const docs = await getDocuments();
      setDocuments(docs);
    } catch (error) {
      toast.error('Failed to fetch documents');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // Poll for PROCESSING documents
  useEffect(() => {
    const processingDocs = documents.filter(d => d.status === 'PROCESSING');
    if (processingDocs.length === 0) return;

    const interval = setInterval(async () => {
      let updated = false;
      for (const doc of processingDocs) {
        try {
          const status = await getDocumentStatus(doc.id);
          if (status.status !== 'PROCESSING') {
            updated = true;
          }
        } catch {
          // ignore
        }
      }
      if (updated) {
        fetchDocuments();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [documents, fetchDocuments]);

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this document?')) return;
    setDeletingId(id);
    try {
      await deleteDocument(id);
      toast.success('Document deleted');
      fetchDocuments();
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Failed to delete document');
    } finally {
      setDeletingId(null);
    }
  };

  const handleVerify = async (id: number) => {
    try {
      await verifyDocument(id);
      toast.success('Document verified successfully');
      fetchDocuments();
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Failed to verify document');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isStale = (doc: Document) => {
    if (!doc.lastVerifiedDate) return true;
    const verifiedDate = new Date(doc.lastVerifiedDate);
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setDate(sixMonthsAgo.getDate() - 180);
    return verifiedDate < sixMonthsAgo;
  };

  const readyCount = documents.filter(d => d.status === 'READY').length;
  const processingCount = documents.filter(d => d.status === 'PROCESSING').length;

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric'
    });
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <div className="min-h-screen">
      {/* Navbar */}
      <nav className="sticky top-0 z-40 glass-card rounded-none border-x-0 border-t-0">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-brand-400 to-purple-500 flex items-center justify-center">
                <Brain className="w-5 h-5 text-white" />
              </div>
              <span className="text-lg font-bold gradient-text">DocMind</span>
            </div>

            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="text-sm text-surface-300">{user?.email}</span>
                <span className="status-pill bg-brand-500/20 text-brand-300 border border-brand-500/30 text-[10px]">
                  {user?.role}
                </span>
              </div>
              <button onClick={handleLogout} className="btn-secondary flex items-center gap-2 text-sm py-2">
                <LogOut className="w-4 h-4" />
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">My Documents</h1>
          <p className="text-surface-400">Upload, manage, and query your documents with AI</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <div className="glass-card p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-brand-500/20 flex items-center justify-center">
              <Files className="w-6 h-6 text-brand-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{documents.length}</p>
              <p className="text-sm text-surface-400">Total Documents</p>
            </div>
          </div>
          <div className="glass-card p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
              <CheckCircle className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{readyCount}</p>
              <p className="text-sm text-surface-400">Ready to Query</p>
            </div>
          </div>
          <div className="glass-card p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-amber-500/20 flex items-center justify-center">
              <Cpu className="w-6 h-6 text-amber-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{processingCount}</p>
              <p className="text-sm text-surface-400">Processing</p>
            </div>
          </div>
        </div>

        {/* Documents Grid */}
        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <div className="flex items-center gap-3 text-surface-400">
              <Cpu className="w-5 h-5 animate-spin" />
              Loading documents...
            </div>
          </div>
        ) : documents.length === 0 ? (
          <div className="glass-card p-16 text-center">
            <div className="w-20 h-20 rounded-2xl bg-brand-500/10 flex items-center justify-center mx-auto mb-6">
              <FileText className="w-10 h-10 text-brand-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">No documents yet</h3>
            <p className="text-surface-400 mb-6 max-w-md mx-auto">
              Upload your first PDF or DOCX to get started with AI-powered document intelligence
            </p>
            <button onClick={() => setIsUploadOpen(true)} className="btn-primary inline-flex items-center gap-2">
              <Plus className="w-4 h-4" />
              Upload Document
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {documents.map((doc) => (
              <div key={doc.id} className="glass-card-hover p-5 flex flex-col animate-fade-in">
                {/* File header */}
                <div className="flex items-start gap-3 mb-4">
                  <div className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 ${
                    doc.fileType === 'PDF'
                      ? 'bg-red-500/20 text-red-400'
                      : 'bg-blue-500/20 text-blue-400'
                  }`}>
                    <FileText className="w-5 h-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="text-sm font-semibold text-white truncate" title={doc.originalName}>
                      {doc.originalName}
                    </h3>
                    <p className="text-xs text-surface-400 mt-0.5">
                      {formatDate(doc.uploadedAt)} · {formatFileSize(doc.fileSizeBytes)}
                    </p>
                  </div>
                </div>

                {/* Status & Info */}
                <div className="flex items-center gap-2 mb-3">
                  <StatusBadge status={doc.status} />
                  {doc.status === 'READY' && (
                    <span className="text-xs text-surface-400 flex items-center gap-1">
                      <Search className="w-3 h-3" />
                      {doc.chunkCount} chunks indexed
                    </span>
                  )}
                </div>

                {/* Stale warning */}
                {doc.status === 'READY' && isStale(doc) && (
                  <div className="flex items-center gap-2 text-xs text-amber-400 bg-amber-500/10 rounded-lg px-3 py-2 mb-3">
                    <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Policy needs verification</span>
                    {(user?.role === 'ADMIN' || user?.role === 'HR') && (
                      <button
                        onClick={() => handleVerify(doc.id)}
                        className="ml-auto text-emerald-400 hover:text-emerald-300 font-medium flex items-center gap-1"
                      >
                        <Shield className="w-3 h-3" />
                        Verify
                      </button>
                    )}
                  </div>
                )}

                {doc.errorMessage && (
                  <p className="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2 mb-3 line-clamp-2">
                    {doc.errorMessage}
                  </p>
                )}

                {/* Actions */}
                <div className="flex gap-2 mt-auto pt-3 border-t border-white/5">
                  {doc.status === 'READY' && (
                    <button
                      onClick={() => navigate(`/query/${doc.id}`)}
                      className="btn-secondary flex-1 flex items-center justify-center gap-2 text-sm py-2"
                    >
                      <MessageSquare className="w-4 h-4" />
                      Ask Questions
                    </button>
                  )}
                  <button
                    onClick={() => handleDelete(doc.id)}
                    disabled={deletingId === doc.id}
                    className="btn-danger flex items-center justify-center gap-1 text-sm py-2 px-3"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* FAB */}
      <button
        onClick={() => setIsUploadOpen(true)}
        className="fixed bottom-8 right-8 w-14 h-14 rounded-full bg-gradient-to-br from-brand-400 to-purple-500
                   shadow-xl shadow-brand-500/30 flex items-center justify-center
                   hover:shadow-2xl hover:shadow-brand-500/40 hover:scale-110 transition-all duration-300
                   active:scale-95 z-30"
      >
        <Plus className="w-6 h-6 text-white" />
      </button>

      <UploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={fetchDocuments}
      />
    </div>
  );
};

export default DashboardPage;
