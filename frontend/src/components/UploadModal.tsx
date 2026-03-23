import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, X, FileText, AlertTriangle, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { uploadDocument } from '../api/documents';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const UploadModal: React.FC<UploadModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      const selectedFile = acceptedFiles[0];
      if (selectedFile.size > 20 * 1024 * 1024) {
        toast.error('File size must be under 20MB');
        return;
      }
      setFile(selectedFile);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    },
    maxFiles: 1,
  });

  const handleUpload = async () => {
    if (!file) return;

    setIsUploading(true);
    try {
      await uploadDocument(file);
      toast.success('Document uploaded successfully! Processing will begin shortly.');
      setFile(null);
      onSuccess();
      onClose();
    } catch (error: any) {
      const message = error.response?.data?.error || 'Failed to upload document';
      toast.error(message);
    } finally {
      setIsUploading(false);
    }
  };

  const handleClose = () => {
    if (!isUploading) {
      setFile(null);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={handleClose} />

      {/* Modal */}
      <div className="glass-card relative z-10 w-full max-w-lg p-8 animate-fade-in">
        {/* Close button */}
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 text-surface-400 hover:text-white transition-colors"
          disabled={isUploading}
        >
          <X className="w-5 h-5" />
        </button>

        <h2 className="text-2xl font-bold mb-2 gradient-text">Upload Document</h2>
        <p className="text-surface-400 text-sm mb-6">
          Drag and drop a PDF or DOCX file to begin AI-powered analysis
        </p>

        {/* Dropzone */}
        <div
          {...getRootProps()}
          className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-300
            ${isDragActive 
              ? 'border-brand-400 bg-brand-500/10' 
              : file 
                ? 'border-emerald-400/50 bg-emerald-500/5' 
                : 'border-white/20 hover:border-brand-400/50 hover:bg-white/5'
            }`}
        >
          <input {...getInputProps()} />
          {file ? (
            <div className="flex flex-col items-center gap-3">
              <div className="w-14 h-14 rounded-xl bg-emerald-500/20 flex items-center justify-center">
                <FileText className="w-7 h-7 text-emerald-400" />
              </div>
              <div>
                <p className="text-white font-medium">{file.name}</p>
                <p className="text-surface-400 text-sm mt-1">
                  {(file.size / 1024 / 1024).toFixed(2)} MB
                </p>
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-3">
              <div className="w-14 h-14 rounded-xl bg-brand-500/20 flex items-center justify-center">
                <Upload className="w-7 h-7 text-brand-400" />
              </div>
              <div>
                <p className="text-white font-medium">
                  {isDragActive ? 'Drop your file here' : 'Drag & drop your document'}
                </p>
                <p className="text-surface-400 text-sm mt-1">
                  or click to browse — PDF & DOCX supported
                </p>
              </div>
            </div>
          )}
        </div>

        {/* File size warning */}
        <div className="flex items-center gap-2 mt-3 text-xs text-surface-400">
          <AlertTriangle className="w-3.5 h-3.5" />
          Maximum file size: 20MB
        </div>

        {/* Actions */}
        <div className="flex gap-3 mt-6">
          <button
            onClick={handleClose}
            className="btn-secondary flex-1"
            disabled={isUploading}
          >
            Cancel
          </button>
          <button
            onClick={handleUpload}
            className="btn-primary flex-1 flex items-center justify-center gap-2"
            disabled={!file || isUploading}
          >
            {isUploading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Uploading...
              </>
            ) : (
              <>
                <Upload className="w-4 h-4" />
                Upload
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default UploadModal;
