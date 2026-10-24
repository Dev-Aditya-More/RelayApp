import { Construction, Trash2, Droplets, Lightbulb, Waves, CircleHelp } from 'lucide-react'

export const inr = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n ?? 0)

export const fmtDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'

export const timeAgo = (iso) => {
  const mins = Math.max(0, Math.round((Date.now() - new Date(iso)) / 60000))
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  if (mins < 60 * 24) return `${Math.round(mins / 60)}h ago`
  return `${Math.round(mins / 1440)}d ago`
}

export const duration = (ms) => {
  const m = Math.floor(Math.abs(ms) / 60000)
  const d = Math.floor(m / 1440), h = Math.floor((m % 1440) / 60), mm = m % 60
  if (d > 0) return `${d}d ${h}h`
  if (h > 0) return `${h}h ${mm}m`
  return `${mm}m`
}

export const STAGES = ['Reported', 'Assigned', 'In progress', 'Resolved', 'Closed']
export const STAGE_INDEX = { OPEN: 0, ASSIGNED: 1, IN_PROGRESS: 2, COMPLETED: 3, CLOSED: 4 }

export const STATUS = {
  OPEN: { label: 'Open', cls: 'bg-amber-100 text-amber-800' },
  ASSIGNED: { label: 'Assigned', cls: 'bg-violet-100 text-violet-800' },
  IN_PROGRESS: { label: 'In progress', cls: 'bg-sky-100 text-sky-800' },
  COMPLETED: { label: 'Resolved', cls: 'bg-emerald-100 text-emerald-800' },
  CLOSED: { label: 'Closed', cls: 'bg-slate-200 text-slate-700' },
}

export const SEVERITY = {
  LOW: { label: 'Low', dot: 'bg-emerald-500', hex: '#10B981' },
  MEDIUM: { label: 'Medium', dot: 'bg-amber-500', hex: '#F59E0B' },
  HIGH: { label: 'High', dot: 'bg-rose-500', hex: '#F43F5E' },
}

export const CATEGORY = {
  POTHOLE: { label: 'Pothole', Icon: Construction, tint: 'bg-orange-100 text-orange-700' },
  GARBAGE: { label: 'Garbage', Icon: Trash2, tint: 'bg-lime-100 text-lime-800' },
  WATER_LEAK: { label: 'Water leak', Icon: Droplets, tint: 'bg-sky-100 text-sky-700' },
  STREETLIGHT: { label: 'Streetlight', Icon: Lightbulb, tint: 'bg-yellow-100 text-yellow-800' },
  DRAINAGE: { label: 'Drainage', Icon: Waves, tint: 'bg-teal-100 text-teal-700' },
  OTHER: { label: 'Other', Icon: CircleHelp, tint: 'bg-slate-100 text-slate-700' },
}

export const PAYMENT_TYPE = {
  FAST_TRACK: 'Fast-track fee',
  FINE: 'SLA penalty',
  CONTRACTOR_PAYOUT: 'Contractor payout',
}

export const ROLE_LABEL = { CITIZEN: 'Citizen', CONTRACTOR: 'Contractor', ADMIN: 'Admin' }
