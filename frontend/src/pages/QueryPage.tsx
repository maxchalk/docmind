import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { askQuestion } from '../api/query';
import { getDocuments } from '../api/documents';
import { Document, ChatMessage, QuerySource } from '../types';
import toast from 'react-hot-toast';
import {
  ArrowLeft, Send, AlertTriangle, ChevronDown, ChevronUp,
  FileText, Brain, User as UserIcon, Clock
} from 'lucide-react';

const QueryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [document, setDocument] = useState<Document | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const fetchDoc = async () => {
      try {
        const docs = await getDocuments();
        const doc = docs.find(d => d.id === Number(id));
        if (doc) {
          setDocument(doc);
        } else {
          toast.error('Document not found');
          navigate('/dashboard');
        }
      } catch {
        toast.error('Failed to load document');
        navigate('/dashboard');
      }
    };
    fetchDoc();
  }, [id, navigate]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const isStale = (doc: Document) => {
    if (!doc.lastVerifiedDate) return true;
    const verifiedDate = new Date(doc.lastVerifiedDate);
    const sixMonthsAgo = new Date();
    sixMonthsAgo.setDate(sixMonthsAgo.getDate() - 180);
    return verifiedDate < sixMonthsAgo;
  };

  const handleSend = async () => {
    const question = input.trim();
    if (!question || isLoading || !id) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      type: 'user',
      content: question,
      timestamp: new Date(),
    };

    const loadingMessage: ChatMessage = {
      id: (Date.now() + 1).toString(),
      type: 'ai',
      content: '',
      timestamp: new Date(),
      isLoading: true,
    };

    setMessages(prev => [...prev, userMessage, loadingMessage]);
    setInput('');
    setIsLoading(true);

    const startTime = Date.now();

    try {
      const response = await askQuestion(Number(id), question);
      const duration = ((Date.now() - startTime) / 1000).toFixed(1);

      const aiMessage: ChatMessage = {
        id: (Date.now() + 2).toString(),
        type: 'ai',
        content: response.answer,
        sources: response.sources,
        isStale: response.stale,
        timestamp: new Date(),
      };

      setMessages(prev => [
        ...prev.slice(0, -1), // Remove loading message
        { ...aiMessage, content: `${aiMessage.content}\n\n_Response time: ${duration}s_` },
      ]);
    } catch (error: any) {
      const errorMsg = error.response?.data?.error || 'Failed to get AI response';
      toast.error(errorMsg);
      setMessages(prev => [
        ...prev.slice(0, -1),
        {
          id: (Date.now() + 2).toString(),
          type: 'ai',
          content: `⚠️ Error: ${errorMsg}`,
          timestamp: new Date(),
        },
      ]);
    } finally {
      setIsLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top bar */}
      <div className="sticky top-0 z-40 glass-card rounded-none border-x-0 border-t-0">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center gap-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="btn-secondary flex items-center gap-2 text-sm py-2 px-3"
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </button>
          <div className="flex items-center gap-2 min-w-0">
            <FileText className="w-5 h-5 text-brand-400 flex-shrink-0" />
            <h1 className="text-lg font-semibold text-white truncate">
              {document?.originalName || 'Loading...'}
            </h1>
          </div>
        </div>
      </div>

      {/* Stale warning banner */}
      {document && isStale(document) && (
        <div className="bg-amber-500/10 border-b border-amber-500/20">
          <div className="max-w-4xl mx-auto px-4 py-3 flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0" />
            <p className="text-sm text-amber-300">
              <strong>Warning:</strong> This document was last verified over 6 months ago.
              Answers may be outdated — please confirm with HR or Admin.
            </p>
          </div>
        </div>
      )}

      {/* Chat area */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-4xl mx-auto px-4 py-6 space-y-6">
          {messages.length === 0 && (
            <div className="text-center py-20">
              <div className="w-20 h-20 rounded-2xl bg-brand-500/10 flex items-center justify-center mx-auto mb-6">
                <Brain className="w-10 h-10 text-brand-400" />
              </div>
              <h2 className="text-xl font-semibold text-white mb-2">Ask anything about this document</h2>
              <p className="text-surface-400 max-w-md mx-auto">
                Ask questions in plain English and get cited answers referencing specific pages
              </p>
              <div className="flex flex-wrap justify-center gap-2 mt-6">
                {['What is the PTO policy?', 'Summarize the key points', 'What are the compliance requirements?'].map((q) => (
                  <button
                    key={q}
                    onClick={() => { setInput(q); inputRef.current?.focus(); }}
                    className="btn-secondary text-sm py-2"
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'} animate-slide-up`}
            >
              {msg.type === 'user' ? (
                <div className="max-w-[80%] flex items-start gap-3">
                  <div className="bg-brand-500/30 backdrop-blur-sm border border-brand-500/20 rounded-2xl rounded-br-md px-5 py-3">
                    <p className="text-white text-sm whitespace-pre-wrap">{msg.content}</p>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-brand-500/20 flex items-center justify-center flex-shrink-0">
                    <UserIcon className="w-4 h-4 text-brand-400" />
                  </div>
                </div>
              ) : (
                <div className="max-w-[85%] flex items-start gap-3">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-purple-500 flex items-center justify-center flex-shrink-0">
                    <Brain className="w-4 h-4 text-white" />
                  </div>
                  <div className="glass-card p-5 rounded-2xl rounded-tl-md">
                    {msg.isLoading ? (
                      <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-brand-400 animate-dot-1" />
                        <div className="w-2 h-2 rounded-full bg-brand-400 animate-dot-2" />
                        <div className="w-2 h-2 rounded-full bg-brand-400 animate-dot-3" />
                      </div>
                    ) : (
                      <>
                        <MarkdownContent content={msg.content} />
                        {msg.sources && msg.sources.length > 0 && (
                          <SourcesSection sources={msg.sources} />
                        )}
                      </>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}

          <div ref={chatEndRef} />
        </div>
      </div>

      {/* Input area */}
      <div className="sticky bottom-0 glass-card rounded-none border-x-0 border-b-0">
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex gap-3">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about this document..."
              className="input-field flex-1"
              disabled={isLoading}
            />
            <button
              onClick={handleSend}
              disabled={!input.trim() || isLoading}
              className="btn-primary flex items-center gap-2 px-5"
            >
              <Send className="w-4 h-4" />
              Send
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ---------------------------------------------------------------------------
// Lightweight markdown renderer — no external dependency required.
// Handles: **bold**, *italic*, _italic_, # headings, - / 1. lists, paragraphs
// ---------------------------------------------------------------------------
const MarkdownContent: React.FC<{ content: string }> = ({ content }) => {
  let keyCounter = 0;
  const k = () => keyCounter++;

  const renderInline = (text: string): React.ReactNode => {
    const parts = text.split(/(\*\*[^*\n]+\*\*|\*[^*\n]+\*|_[^_\n]+_)/g);
    return parts.map((seg, i) => {
      if (seg.startsWith('**') && seg.endsWith('**') && seg.length > 4)
        return <strong key={i} className="font-semibold text-white">{seg.slice(2, -2)}</strong>;
      if (seg.length > 2 &&
          ((seg.startsWith('*') && seg.endsWith('*')) ||
           (seg.startsWith('_') && seg.endsWith('_'))))
        return <em key={i} className="text-surface-300 not-italic opacity-80">{seg.slice(1, -1)}</em>;
      return <span key={i}>{seg}</span>;
    });
  };

  const lines = content.split('\n');
  const elements: React.ReactNode[] = [];
  let listType: 'ul' | 'ol' | null = null;
  let listItems: string[] = [];

  const flushList = () => {
    if (!listItems.length) return;
    if (listType === 'ol') {
      elements.push(
        <ol key={k()} className="list-decimal pl-5 my-2 space-y-1">
          {listItems.map((item, i) => (
            <li key={i} className="text-sm leading-relaxed text-surface-200">{renderInline(item)}</li>
          ))}
        </ol>
      );
    } else {
      elements.push(
        <ul key={k()} className="list-disc pl-5 my-2 space-y-1">
          {listItems.map((item, i) => (
            <li key={i} className="text-sm leading-relaxed text-surface-200">{renderInline(item)}</li>
          ))}
        </ul>
      );
    }
    listItems = [];
    listType = null;
  };

  lines.forEach(line => {
    const t = line.trimEnd();

    if (t.startsWith('### ')) { flushList(); elements.push(<h3 key={k()} className="text-sm font-bold text-white mt-3 mb-1">{t.slice(4)}</h3>); return; }
    if (t.startsWith('## '))  { flushList(); elements.push(<h2 key={k()} className="text-base font-bold text-white mt-3 mb-1">{t.slice(3)}</h2>); return; }
    if (t.startsWith('# '))   { flushList(); elements.push(<h1 key={k()} className="text-lg font-bold text-white mt-3 mb-1">{t.slice(2)}</h1>); return; }

    const ulMatch = t.match(/^[-*•]\s+(.+)/);
    if (ulMatch) { if (listType === 'ol') flushList(); listType = 'ul'; listItems.push(ulMatch[1]); return; }

    const olMatch = t.match(/^\d+[.)]\s+(.+)/);
    if (olMatch) { if (listType === 'ul') flushList(); listType = 'ol'; listItems.push(olMatch[1]); return; }

    flushList();
    if (t === '') { if (elements.length) elements.push(<div key={k()} className="h-2" />); return; }

    elements.push(<p key={k()} className="text-sm leading-relaxed text-surface-200">{renderInline(t)}</p>);
  });

  flushList();
  return <div className="space-y-0.5">{elements}</div>;
};

// Collapsible sources component
const SourcesSection: React.FC<{ sources: QuerySource[] }> = ({ sources }) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="mt-4 pt-3 border-t border-white/10">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 text-xs text-surface-400 hover:text-brand-400 transition-colors"
      >
        <FileText className="w-3.5 h-3.5" />
        <span>Sources ({sources.length})</span>
        {isOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
      </button>

      {isOpen && (
        <div className="mt-3 space-y-2 animate-fade-in">
          {sources.map((source, i) => (
            <div key={i} className="bg-white/5 rounded-lg px-3 py-2 text-xs">
              <span className="text-brand-400 font-semibold">Page {source.pageNumber}</span>
              <span className="text-surface-400 ml-1">— "{source.snippet}..."</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default QueryPage;
