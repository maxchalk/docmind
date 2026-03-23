import React from 'react';
import { Loader2, CheckCircle2, XCircle } from 'lucide-react';

interface StatusBadgeProps {
  status: string;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  switch (status) {
    case 'PROCESSING':
      return (
        <span className="status-pill bg-amber-500/20 text-amber-300 border border-amber-500/30">
          <Loader2 className="w-3 h-3 animate-spin" />
          Processing...
        </span>
      );
    case 'READY':
      return (
        <span className="status-pill bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
          <CheckCircle2 className="w-3 h-3" />
          Ready
        </span>
      );
    case 'FAILED':
      return (
        <span className="status-pill bg-red-500/20 text-red-300 border border-red-500/30">
          <XCircle className="w-3 h-3" />
          Failed
        </span>
      );
    default:
      return (
        <span className="status-pill bg-surface-500/20 text-surface-300">
          {status}
        </span>
      );
  }
};

export default StatusBadge;
