import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PlusCircle, Search } from 'lucide-react'
import api from '../api'
import { useAuth } from '../auth'
import { Empty, Spinner } from '../components/Bits'
import OrderCard from '../components/OrderCard'
import { STATUS } from '../utils'

const FILTERS = ['ALL', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CLOSED']

export default function Orders() {
  const { user } = useAuth()
  const [orders, setOrders] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [q, setQ] = useState('')
  const [breachedOnly, setBreachedOnly] = useState(false)

  useEffect(() => { api.get('/work-orders').then((r) => setOrders(r.data)) }, [])
  if (!orders) return <Spinner />

  const shown = orders.filter((o) =>
    (filter === 'ALL' || o.status === filter) &&
    (!breachedOnly || o.slaBreached) &&
    `${o.title} ${o.code} ${o.area}`.toLowerCase().includes(q.toLowerCase()))

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <h1 className="font-display text-3xl font-extrabold sm:text-4xl">{user.role === 'CITIZEN' ? 'My reports' : 'Work orders'}</h1>
        {user.role === 'CITIZEN' && <Link to="/report" className="btn-primary"><PlusCircle size={16} />Report an issue</Link>}
      </div>

      <div className="mb-5 flex flex-wrap items-center gap-2">
        <div className="relative min-w-[220px] flex-1 sm:max-w-xs">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-300" />
          <input className="input !pl-9" placeholder="Search title, code or area" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        {FILTERS.map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={`chip !px-3.5 !py-2 transition ${filter === f ? 'bg-ink text-white' : 'bg-white text-ink-700 ring-1 ring-ink/10 hover:bg-paper-200'}`}>
            {f === 'ALL' ? 'All' : STATUS[f].label}
            <span className="opacity-60">{f === 'ALL' ? orders.length : orders.filter((o) => o.status === f).length}</span>
          </button>
        ))}
        <button onClick={() => setBreachedOnly(!breachedOnly)}
          className={`chip !px-3.5 !py-2 transition ${breachedOnly ? 'bg-rose-600 text-white' : 'bg-white text-rose-700 ring-1 ring-rose-200 hover:bg-rose-50'}`}>
          SLA breached
        </button>
      </div>

      {shown.length === 0
        ? <Empty title="No work orders match" hint="Try a different filter or search term." />
        : <div className="grid gap-4 lg:grid-cols-2">{shown.map((o) => <OrderCard key={o.id} order={o} showRisk={user.role === 'ADMIN'} />)}</div>}
    </div>
  )
}
