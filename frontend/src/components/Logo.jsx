/** The Relay baton: a rounded bar tilted mid-handoff, with motion arcs trailing behind it. */
export function Baton({ size = 28, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" className={className} aria-hidden>
      <g transform="rotate(-35 32 32)">
        <rect x="8" y="25" width="48" height="14" rx="7" fill="#FF5B2E" />
        <rect x="40" y="25" width="7" height="14" fill="#14172B" opacity=".22" />
        <rect x="17" y="25" width="3" height="14" fill="#fff" opacity=".35" />
      </g>
    </svg>
  )
}

export default function Logo({ dark = false, size = 30 }) {
  return (
    <div className="flex items-center gap-2.5">
      <span className={`grid place-items-center rounded-xl ${dark ? 'bg-white/10' : 'bg-ink'}`} style={{ width: size + 10, height: size + 10 }}>
        <Baton size={size} />
      </span>
      <span className={`font-display text-2xl font-extrabold tracking-tight ${dark ? 'text-white' : 'text-ink'}`}>
        relay<span className="text-baton">.</span>
      </span>
    </div>
  )
}
