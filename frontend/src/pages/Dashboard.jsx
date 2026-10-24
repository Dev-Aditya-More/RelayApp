import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlarmClock, ArrowRight, Gauge, HardHat, Inbox, PlusCircle, Star, Timer, TrendingUp, Wallet } from 'lucide-react'
import api from '../api'
import { useAuth } from '../auth'
import { Empty, RiskMeter, SeverityTag, Spinner, StatusBadge } from '../components/Bits'
import OrderCard from '../components/OrderCard'
import { CATEGORY, SEVERITY, inr, STATUS } from '../utils'

const Stat = ({ label, value, sub, Icon, tone = 'bg-ink text-white' }) => (
  <div className="card rise p-4">
    <div className="flex items-start justify-between">
      <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">{label}</p>
      <span className={`grid h-8 w-8 place-items-center rounded-lg ${tone}`}><Icon size={16} /></span>
    </div>
    <p className="mt-2 font-display text-3xl font-extrabold">{value}</p>
    {sub && <p className="mt-0.5 text-xs text-ink-500">{sub}</p>}
  </div>
)

/** Issue map without map tiles: every report is a glow on a scaled city grid. */
function Hotspots({ points }) {
  const view = useMemo(() => {
    if (!points.length) return null
    const lats = points.map((p) => p.lat), lngs = points.map((p) => p.lng)
    const [minLa, maxLa, minLo, maxLo] = [Math.min(...lats), Math.max(...lats), Math.min(...lngs), Math.max(...lngs)]
    const pad = 28, w = 520, h = 300
    return points.map((p) => ({
      ...p,
      x: pad + ((p.lng - minLo) / (maxLo - minLo || 1)) * (w - pad * 2),
      y: pad + (1 - (p.lat - minLa) / (maxLa - minLa || 1)) * (h - pad * 2),
    }))
  }, [points])

  return (
    <svg viewBox="0 0 520 300" className="w-full rounded-xl bg-ink">
      <defs>
        {Object.entries(SEVERITY).map(([k, v]) => (
          <radialGradient id={`g-${k}`} key={k}><stop offset="0%" stopColor={v.hex} stopOpacity=".9" /><stop offset="100%" stopColor={v.hex} stopOpacity="0" /></radialGradient>
        ))}
      </defs>
      {[...Array(9)].map((_, i) => <line key={`v${i}`} x1={i * 65} y1="0" x2={i * 65} y2="300" stroke="#fff" strokeOpacity=".05" />)}
      {[...Array(6)].map((_, i) => <line key={`h${i}`} x1="0" y1={i * 60} x2="520" y2={i * 60} stroke="#fff" strokeOpacity=".05" />)}
      {view?.map((p, i) => (
        <g key={i} opacity={p.status === 'CLOSED' ? 0.45 : 1}>
          <circle cx={p.x} cy={p.y} r="34" fill={`url(#g-${p.severity})`} />
          <circle cx={p.x} cy={p.y} r="4" fill={SEVERITY[p.severity].hex} stroke="#fff" strokeWidth="1.5" />
        </g>
      ))}
    </svg>
  )
}

function Bars({ data, colorFor }) {
  const max = Math.max(1, ...Object.values(data))
  return (
    <div className="space-y-2.5">
      {Object.entries(data).map(([k, v]) => (
        <div key={k}>
          <div className="mb-1 flex justify-between text-xs font-semibold"><span>{colorFor(k).label}</span><span className="text-ink-500">{v}</span></div>
          <div className="h-2 rounded-full bg-ink/10"><div className="h-full rounded-full" style={{ width: `${(v / max) * 100}%`, background: colorFor(k).color }} /></div>
        </div>
      ))}
    </div>
  )
}

function AdminView() {
  const [a, setA] = useState(null)
  useEffect(() => { api.get('/analytics').then((r) => setA(r.data)) }, [])
  if (!a) return <Spinner />
  const trendMax = Math.max(1, ...a.trend.map((t) => t.count))
  const catColor = { POTHOLE: '#FB923C', GARBAGE: '#84CC16', WATER_LEAK: '#38BDF8', STREETLIGHT: '#FACC15', DRAINAGE: '#14B8A6', OTHER: '#94A3B8' }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <Stat label="Awaiting hand-off" value={a.open} sub="unassigned reports" Icon={Inbox} tone="bg-amber-100 text-amber-700" />
        <Stat label="In motion" value={a.active} sub="assigned or in progress" Icon={HardHat} tone="bg-sky-100 text-sky-700" />
        <Stat label="SLA compliance" value={`${a.slaCompliance}%`} sub={`${a.resolved} resolved`} Icon={Gauge} tone="bg-emerald-100 text-emerald-700" />
        <Stat label="Avg resolution" value={`${a.avgResolutionHours}h`} sub="report to resolved" Icon={Timer} tone="bg-violet-100 text-violet-700" />
        <Stat label="SLA breaches" value={a.breached} sub="across all orders" Icon={AlarmClock} tone="bg-rose-100 text-rose-700" />
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="card p-5 lg:col-span-3">
          <div className="mb-3 flex items-center justify-between"><h3 className="font-display text-lg font-bold">Hotspot map</h3>
            <div className="flex gap-3">{Object.entries(SEVERITY).map(([k, v]) => <span key={k} className="flex items-center gap-1 text-xs text-ink-500"><span className={`h-2 w-2 rounded-full ${v.dot}`} />{v.label}</span>)}</div></div>
          <Hotspots points={a.points} />
          <div className="mt-3 flex flex-wrap gap-2">{a.hotspots.map((h) => <span key={h.area} className="chip bg-ink/5 text-ink-700">{h.area} · {h.count}</span>)}</div>
        </div>
        <div className="card p-5 lg:col-span-2">
          <h3 className="mb-3 font-display text-lg font-bold">Reports, last 7 days</h3>
          <div className="flex h-32 items-end gap-2">
            {a.trend.map((t) => (
              <div key={t.day} className="flex flex-1 flex-col items-center gap-1">
                <span className="text-[10px] font-bold text-ink-500">{t.count || ''}</span>
                <div className="w-full rounded-t-md bg-baton" style={{ height: `${Math.max(4, (t.count / trendMax) * 100)}%`, opacity: t.count ? 1 : 0.2 }} />
                <span className="text-[10px] font-semibold text-ink-500">{t.day}</span>
              </div>
            ))}
          </div>
          <h3 className="mb-3 mt-6 font-display text-lg font-bold">By category</h3>
          <Bars data={a.byCategory} colorFor={(k) => ({ label: CATEGORY[k].label, color: catColor[k] })} />
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="card p-5 lg:col-span-3">
          <div className="mb-3 flex items-center gap-2"><TrendingUp size={18} className="text-baton" /><h3 className="font-display text-lg font-bold">Likely to miss their SLA</h3></div>
          {a.atRisk.length === 0 ? <p className="py-6 text-center text-sm text-ink-500">Nothing above 60% risk. Nice.</p> : (
            <div className="divide-y divide-ink/10">
              {a.atRisk.map((o) => (
                <Link to={`/orders/${o.id}`} key={o.id} className="flex items-center gap-3 py-3 hover:opacity-80">
                  <div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{o.title}</p><p className="text-xs text-ink-500">{o.code} · {o.area} · <StatusBadge status={o.status} /></p></div>
                  <RiskMeter value={o.delayRisk} />
                </Link>
              ))}
            </div>
          )}
        </div>
        <div className="card p-5 lg:col-span-2">
          <div className="mb-3 flex items-center gap-2"><Wallet size={18} className="text-baton" /><h3 className="font-display text-lg font-bold">Money moved</h3></div>
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between"><dt className="text-ink-500">Fast-track fees</dt><dd className="font-bold">{inr(a.revenue.fastTrack)}</dd></div>
            <div className="flex justify-between"><dt className="text-ink-500">SLA penalties collected</dt><dd className="font-bold">{inr(a.revenue.fines)}</dd></div>
            <div className="flex justify-between border-t border-ink/10 pt-3"><dt className="text-ink-500">Contractor payouts</dt><dd className="font-bold">{inr(a.revenue.payouts)}</dd></div>
          </dl>
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-paper-200 p-3 text-sm"><Star size={16} className="text-baton" /> Avg citizen rating <b className="ml-auto">{a.avgRating || '—'} / 5</b></div>
        </div>
      </div>

      <div className="card overflow-x-auto p-5">
        <h3 className="mb-3 font-display text-lg font-bold">Contractor scoreboard</h3>
        <table className="w-full min-w-[520px] text-sm">
          <thead><tr className="text-left text-xs uppercase tracking-wide text-ink-500"><th className="pb-2">Contractor</th><th>Active</th><th>Completed</th><th>On time</th><th>Rating</th></tr></thead>
          <tbody className="divide-y divide-ink/10">
            {a.contractors.map((c) => (
              <tr key={c.id}><td className="py-2.5 font-bold">{c.name}</td><td>{c.active}</td><td>{c.completed}</td>
                <td><span className={`chip ${c.onTime >= 80 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>{c.onTime}%</span></td>
                <td>{c.avgRating || '—'}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function ListView({ role }) {
  const [orders, setOrders] = useState(null)
  const [payments, setPayments] = useState([])
  useEffect(() => {
    api.get('/work-orders').then((r) => setOrders(r.data))
    api.get('/payments').then((r) => setPayments(r.data))
  }, [])
  if (!orders) return <Spinner />

  const active = orders.filter((o) => !o.completedAt)
  const owed = payments.filter((p) => p.status !== 'PAID' && p.type === 'FINE')
  const earned = payments.filter((p) => p.type === 'CONTRACTOR_PAYOUT' && p.status === 'PAID').reduce((s, p) => s + p.amount, 0)
  const toRate = orders.filter((o) => o.status === 'COMPLETED')

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Stat label={role === 'CITIZEN' ? 'Your reports' : 'Assigned to you'} value={orders.length} Icon={Inbox} tone="bg-ink/10 text-ink" />
        <Stat label="In motion" value={active.length} Icon={HardHat} tone="bg-sky-100 text-sky-700" />
        {role === 'CITIZEN'
          ? <Stat label="Waiting for your rating" value={toRate.length} Icon={Star} tone="bg-baton-100 text-baton-600" />
          : <Stat label="Earned" value={inr(earned)} Icon={Wallet} tone="bg-emerald-100 text-emerald-700" />}
        {role === 'CITIZEN'
          ? <Stat label="Resolved" value={orders.filter((o) => o.completedAt).length} Icon={Gauge} tone="bg-emerald-100 text-emerald-700" />
          : <Stat label="Penalties due" value={owed.length} sub={owed.length ? inr(owed.reduce((s, p) => s + p.amount, 0)) : 'all clear'} Icon={AlarmClock} tone="bg-rose-100 text-rose-700" />}
      </div>

      {role === 'CONTRACTOR' && owed.length > 0 && (
        <div className="flex items-center justify-between gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm">
          <span><b>{owed.length} SLA penalty</b> waiting for payment.</span>
          <Link to="/payments" className="btn-danger !py-2">Review <ArrowRight size={14} /></Link>
        </div>
      )}
      {role === 'CITIZEN' && toRate.length > 0 && (
        <div className="flex items-center justify-between gap-3 rounded-2xl border border-baton/30 bg-baton-50 p-4 text-sm">
          <span><b>{toRate[0].title}</b> was resolved. How did it go?</span>
          <Link to={`/orders/${toRate[0].id}`} className="btn-primary !py-2">Rate it</Link>
        </div>
      )}

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-display text-xl font-bold">{role === 'CITIZEN' ? 'Your reports in motion' : 'Your queue'}</h3>
          <Link to="/orders" className="text-sm font-bold text-baton-600 hover:underline">View all</Link>
        </div>
        {active.length === 0 ? (
          <Empty title={role === 'CITIZEN' ? 'No open reports' : 'Nothing in your queue'} hint={role === 'CITIZEN' ? 'Spot a pothole, a leak or a dead streetlight? Pass it on.' : 'New hand-offs from the admin will land here.'}>
            {role === 'CITIZEN' && <Link to="/report" className="btn-primary"><PlusCircle size={16} />Report an issue</Link>}
          </Empty>
        ) : <div className="grid gap-4 lg:grid-cols-2">{active.slice(0, 6).map((o) => <OrderCard key={o.id} order={o} />)}</div>}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const greeting = { ADMIN: 'Command centre', CONTRACTOR: 'Your runs today', CITIZEN: 'Your neighbourhood, handled' }[user.role]
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-baton-600">Hi {user.name.split(' ')[0]} 👋</p>
          <h1 className="font-display text-3xl font-extrabold sm:text-4xl">{greeting}</h1>
        </div>
        {user.role === 'CITIZEN' && <Link to="/report" className="btn-primary"><PlusCircle size={16} />Report an issue</Link>}
      </div>
      {user.role === 'ADMIN' ? <AdminView /> : <ListView role={user.role} />}
    </div>
  )
}
