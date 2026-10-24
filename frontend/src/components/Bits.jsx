import { useEffect, useState } from 'react'
import { AlarmClock, CheckCircle2, X, Zap } from 'lucide-react'
import { CATEGORY, SEVERITY, STATUS, duration } from '../utils'

export const StatusBadge = ({ status }) => (
  <span className={`chip ${STATUS[status].cls}`}>{STATUS[status].label}</span>
)

export const SeverityTag = ({ severity }) => (
  <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-ink-500">
    <span className={`h-2 w-2 rounded-full ${SEVERITY[severity].dot}`} />
    {SEVERITY[severity].label}
  </span>
)

export function CategoryIcon({ category, size = 18 }) {
  const { Icon, tint } = CATEGORY[category]
  return (
    <span className={`grid shrink-0 place-items-center rounded-xl ${tint}`} style={{ width: size + 18, height: size + 18 }}>
      <Icon size={size} />
    </span>
  )
}

export const FastTrackTag = () => (
  <span className="chip bg-baton-100 text-baton-600"><Zap size={12} /> Fast-track</span>
)

/** Live SLA countdown, or the final outcome for resolved work. */
export function SlaTimer({ order }) {
  const [, tick] = useState(0)
  useEffect(() => {
    const id = setInterval(() => tick((n) => n + 1), 30000)
    return () => clearInterval(id)
  }, [])

  if (order.completedAt) {
    return order.slaBreached
      ? <span className="chip bg-rose-100 text-rose-700"><AlarmClock size={12} /> Missed SLA</span>
      : <span className="chip bg-emerald-100 text-emerald-700"><CheckCircle2 size={12} /> Met SLA</span>
  }
  const left = new Date(order.slaDeadline) - Date.now()
  if (order.slaBreached || left <= 0) {
    return <span className="chip bg-rose-100 text-rose-700"><AlarmClock size={12} /> Breached {duration(left)} ago</span>
  }
  const tight = left < 4 * 3600 * 1000
  return (
    <span className={`chip ${tight ? 'bg-amber-100 text-amber-800' : 'bg-ink/5 text-ink-700'}`}>
      <AlarmClock size={12} /> {duration(left)} left
    </span>
  )
}

export function RiskMeter({ value }) {
  if (value == null) return null
  const tone = value >= 70 ? 'bg-rose-500' : value >= 45 ? 'bg-amber-500' : 'bg-emerald-500'
  return (
    <div className="min-w-[110px]">
      <div className="mb-1 flex justify-between text-[11px] font-semibold text-ink-500">
        <span>Delay risk</span><span>{value}%</span>
      </div>
      <div className="h-1.5 rounded-full bg-ink/10"><div className={`h-full rounded-full ${tone}`} style={{ width: `${value}%` }} /></div>
    </div>
  )
}

export function Modal({ title, onClose, children, wide = false }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-ink/50 p-4 backdrop-blur-sm" onMouseDown={onClose}>
      <div className={`rise card max-h-[90vh] w-full overflow-auto p-6 ${wide ? 'max-w-2xl' : 'max-w-md'}`} onMouseDown={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-bold">{title}</h3>
          <button onClick={onClose} className="rounded-lg p-1 text-ink-500 hover:bg-ink/5" aria-label="Close"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

export const Empty = ({ title, hint, children }) => (
  <div className="card grid place-items-center px-6 py-14 text-center">
    <div className="mb-3 flex gap-1 opacity-30">
      <span className="h-2 w-8 -rotate-12 rounded-full bg-baton" />
      <span className="h-2 w-8 -rotate-12 rounded-full bg-ink" />
    </div>
    <p className="font-display text-lg font-bold">{title}</p>
    {hint && <p className="mt-1 max-w-sm text-sm text-ink-500">{hint}</p>}
    {children && <div className="mt-4">{children}</div>}
  </div>
)

export const Spinner = () => (
  <div className="grid place-items-center py-20">
    <div className="h-8 w-8 animate-spin rounded-full border-4 border-ink/10 border-t-baton" />
  </div>
)

export const Stars = ({ value, onChange, size = 24 }) => (
  <div className="flex gap-1">
    {[1, 2, 3, 4, 5].map((n) => (
      <button
        key={n} type="button" disabled={!onChange} onClick={() => onChange?.(n)}
        className={`transition ${onChange ? 'hover:scale-110' : ''} ${n <= value ? 'text-baton' : 'text-ink/15'}`}
        aria-label={`${n} star`}
      >
        <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor"><path d="m12 2 3 6.9 7.5.7-5.7 5 1.7 7.4L12 18l-6.5 4 1.7-7.4-5.7-5L9 8.9z" /></svg>
      </button>
    ))}
  </div>
)
