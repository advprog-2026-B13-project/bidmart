"use client";

import { useState, createContext, useContext, useCallback } from "react";
import { Check, X, AlertTriangle } from "lucide-react";

type ToastType = "success" | "error" | "info";

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue>({ showToast: () => {} });

export function useToast() {
  return useContext(ToastContext);
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: ToastType = "success") => {
    const id = Math.random().toString(36).slice(2);
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-6 right-6 z-[100] flex flex-col gap-3 pointer-events-none">
        {toasts.map(toast => (
          <ToastItem key={toast.id} toast={toast} onDismiss={() => {
            setToasts(prev => prev.filter(t => t.id !== toast.id));
          }} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: () => void }) {
  const icons = {
    success: <Check className="w-5 h-5" />,
    error: <X className="w-5 h-5" />,
    info: <AlertTriangle className="w-5 h-5" />,
  };

  const styles = {
    success: "bg-electric text-white border-electric",
    error: "bg-hot text-white border-hot",
    info: "bg-acid text-black border-acid",
  };

  return (
    <div className={`flex items-center gap-3 px-5 py-4 border-3 border-black shadow-[5px_5px_0_#0A0A0A] pointer-events-auto animate-fade-in-up ${styles[toast.type]}`}>
      {icons[toast.type]}
      <span className="font-black text-sm uppercase tracking-tight">{toast.message}</span>
      <button onClick={onDismiss} className="ml-2 hover:opacity-70 transition-opacity">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}
