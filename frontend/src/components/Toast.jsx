import { createContext, useCallback, useContext, useState } from 'react'
import { CheckCircle2, AlertTriangle } from 'lucide-react'

const ToastContext = createContext(() => {})

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const push = useCallback((message, kind = 'ok') => {
    const id = Math.random()
    setToasts((t) => [...t, { id, message, kind }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4200)
  }, [])

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex w-[min(92vw,360px)] flex-col gap-2">
        {toasts.map((t) => (
          <div key={t.id} className="rise flex items-start gap-3 rounded-xl bg-ink px-4 py-3 text-sm text-white shadow-xl">
            {t.kind === 'ok'
              ? <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-400" />
              : <AlertTriangle size={18} className="mt-0.5 shrink-0 text-baton" />}
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
