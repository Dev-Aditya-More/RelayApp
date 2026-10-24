import { Link } from 'react-router-dom'
import { MapPin } from 'lucide-react'
import { CategoryIcon, FastTrackTag, RiskMeter, SeverityTag, SlaTimer, StatusBadge } from './Bits'
import RelayTrack from './RelayTrack'
import { CATEGORY, timeAgo } from '../utils'

export default function OrderCard({ order, showRisk = false }) {
  const active = !order.completedAt
  return (
    <Link to={`/orders/${order.id}`} className="card rise block p-4 transition hover:-translate-y-0.5 hover:shadow-lg">
      <div className="flex items-start gap-3">
        <CategoryIcon category={order.category} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-mono text-xs font-bold text-ink-500">{order.code}</span>
            <StatusBadge status={order.status} />
            {order.fastTrack && <FastTrackTag />}
          </div>
          <h3 className="mt-1 truncate font-display text-base font-bold">{order.title}</h3>
          <p className="mt-0.5 flex flex-wrap items-center gap-x-3 text-xs text-ink-500">
            <span className="inline-flex items-center gap-1"><MapPin size={12} />{order.area}</span>
            <span>{CATEGORY[order.category].label}</span>
            <SeverityTag severity={order.severity} />
            <span>{timeAgo(order.createdAt)}</span>
          </p>
        </div>
        <div className="hidden shrink-0 flex-col items-end gap-2 sm:flex">
          <SlaTimer order={order} />
          {showRisk && active && <RiskMeter value={order.delayRisk} />}
        </div>
      </div>
      <div className="mt-3"><RelayTrack status={order.status} compact breached={order.slaBreached} /></div>
      <div className="mt-2 flex items-center justify-between text-xs text-ink-500 sm:hidden"><SlaTimer order={order} /></div>
      {order.contractor && <p className="mt-2 text-xs text-ink-500">Carried by <b className="text-ink">{order.contractor.name}</b></p>}
    </Link>
  )
}
