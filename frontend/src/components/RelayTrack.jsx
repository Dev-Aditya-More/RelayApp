import { Check } from 'lucide-react'
import { Baton } from './Logo'
import { STAGES, STAGE_INDEX } from '../utils'

/**
 * Signature element: a work order is a relay race. Each status change is a hand-off,
 * and the baton marks whoever is carrying the issue right now.
 */
export default function RelayTrack({ status, compact = false, breached = false }) {
  const current = STAGE_INDEX[status] ?? 0
  const pct = (current / (STAGES.length - 1)) * 100

  return (
    <div className={compact ? 'pt-1' : 'pt-2'}>
      <div className="relative mx-3">
        <div className="absolute left-0 right-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-ink/10" />
        <div
          className={`absolute left-0 top-1/2 h-1 -translate-y-1/2 rounded-full transition-all duration-700 ${breached ? 'bg-rose-500' : 'bg-baton'}`}
          style={{ width: `${pct}%` }}
        />
        <div className="relative flex justify-between">
          {STAGES.map((label, i) => {
            const done = i < current
            const here = i === current
            return (
              <div key={label} className="flex flex-col items-center">
                <span
                  className={`grid place-items-center rounded-full border-2 transition ${compact ? 'h-4 w-4' : 'h-7 w-7'} ${
                    here ? 'baton-pulse border-baton bg-white' : done ? `${breached ? 'border-rose-500 bg-rose-500' : 'border-baton bg-baton'} text-white` : 'border-ink/15 bg-paper'
                  }`}
                >
                  {here ? <Baton size={compact ? 14 : 22} /> : done && !compact ? <Check size={14} strokeWidth={3} /> : null}
                </span>
              </div>
            )
          })}
        </div>
      </div>
      {!compact && (
        <div className="mt-2 flex justify-between text-[11px] font-medium text-ink-500">
          {STAGES.map((s, i) => (
            <span key={s} className={`w-16 text-center ${i === current ? 'font-bold text-ink' : ''}`}>{s}</span>
          ))}
        </div>
      )}
    </div>
  )
}
