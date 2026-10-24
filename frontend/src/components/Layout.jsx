import { useCallback, useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Bell, ClipboardList, LayoutDashboard, LogOut, Menu, PlusCircle, ReceiptIndianRupee, Timer, Users, X } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useToast } from './Toast'
import { useAuth } from '../auth'
import { ROLE_LABEL, timeAgo } from '../utils'
import Logo from './Logo'

function Bell_() {
  const [items, setItems] = useState([])
  const [open, setOpen] = useState(false)
  const nav = useNavigate()
  const box = useRef(null)

  const load = useCallback(() => api.get('/notifications').then((r) => setItems(r.data)).catch(() => {}), [])
  useEffect(() => {
    load()
    const id = setInterval(load, 30000)
    return () => clearInterval(id)
  }, [load])
  useEffect(() => {
    const close = (e) => box.current && !box.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const unseen = items.filter((n) => !n.seen).length
  const toggle = () => {
    setOpen((o) => !o)
    if (!open && unseen) api.post('/notifications/read-all').then(() => setTimeout(load, 2500))
  }

  return (
    <div className="relative" ref={box}>
      <button onClick={toggle} className="relative rounded-xl border border-ink/10 bg-white p-2.5 hover:bg-paper-200" aria-label="Notifications">
        <Bell size={18} />
        {unseen > 0 && <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-baton px-1 text-[10px] font-bold text-white">{unseen}</span>}
      </button>
      {open && (
        <div className="rise card absolute right-0 z-40 mt-2 max-h-96 w-80 overflow-auto p-2">
          {items.length === 0 && <p className="p-4 text-center text-sm text-ink-500">Nothing yet. Hand-offs show up here.</p>}
          {items.map((n) => (
            <button key={n.id} onClick={() => { setOpen(false); n.workOrderId && nav(`/orders/${n.workOrderId}`) }}
              className="flex w-full gap-2 rounded-xl p-3 text-left text-sm hover:bg-paper-200">
              <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${n.seen ? 'bg-ink/15' : 'bg-baton'}`} />
              <span><span className="block leading-snug">{n.message}</span><span className="text-xs text-ink-500">{timeAgo(n.createdAt)}</span></span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function VerifyBanner({ email }) {
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const resend = async () => {
    setBusy(true)
    try { await api.post('/auth/resend-verification'); toast('Verification email sent') }
    catch (e) { toast(errorMessage(e), 'err') } finally { setBusy(false) }
  }
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-baton/30 bg-baton-50 px-4 py-3 text-sm font-medium text-baton-600 lg:px-8">
      <span>Verify your email to report issues or receive work. We sent a link to <b>{email}</b>.</span>
      <button className="font-bold underline disabled:opacity-60" onClick={resend} disabled={busy}>{busy ? 'Sending…' : 'Resend email'}</button>
    </div>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const [menu, setMenu] = useState(false)
  const nav = useNavigate()

  const links = [
    { to: '/', label: 'Overview', Icon: LayoutDashboard, end: true },
    ...(user.role === 'CITIZEN' ? [{ to: '/report', label: 'Report an issue', Icon: PlusCircle }] : []),
    { to: '/orders', label: user.role === 'CITIZEN' ? 'My reports' : 'Work orders', Icon: ClipboardList },
    { to: '/payments', label: 'Payments', Icon: ReceiptIndianRupee },
    { to: '/sla', label: 'SLA policies', Icon: Timer },
    ...(user.role === 'ADMIN' ? [{ to: '/people', label: 'People', Icon: Users }] : []),
  ]

  const sidebar = (
    <div className="flex h-full flex-col bg-ink p-5 text-white">
      <div className="mb-8 flex items-center justify-between"><Logo dark /><button className="lg:hidden" onClick={() => setMenu(false)}><X /></button></div>
      <nav className="flex flex-1 flex-col gap-1">
        {links.map(({ to, label, Icon, end }) => (
          <NavLink key={to} to={to} end={end} onClick={() => setMenu(false)}
            className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition ${isActive ? 'bg-baton text-white shadow-[0_8px_20px_-8px_rgba(255,91,46,.9)]' : 'text-ink-300 hover:bg-white/5 hover:text-white'}`}>
            <Icon size={18} />{label}
          </NavLink>
        ))}
      </nav>
      <div className="rounded-2xl bg-white/5 p-4">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-baton font-display text-lg font-bold">{user.name[0]}</span>
          <div className="min-w-0"><p className="truncate text-sm font-bold">{user.name}</p><p className="text-xs text-ink-300">{ROLE_LABEL[user.role]}</p></div>
        </div>
        <button onClick={() => { logout(); nav('/login') }} className="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border border-white/10 py-2 text-xs font-semibold text-ink-300 hover:bg-white/5 hover:text-white">
          <LogOut size={14} /> Sign out
        </button>
      </div>
    </div>
  )

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[260px_1fr]">
      <aside className="sticky top-0 hidden h-screen lg:block">{sidebar}</aside>
      {menu && <div className="fixed inset-0 z-40 bg-ink/50 lg:hidden" onClick={() => setMenu(false)}><div className="h-full w-72" onClick={(e) => e.stopPropagation()}>{sidebar}</div></div>}
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex items-center justify-between border-b border-ink/10 bg-paper/85 px-4 py-3 backdrop-blur lg:px-8">
          <button className="rounded-xl border border-ink/10 bg-white p-2.5 lg:hidden" onClick={() => setMenu(true)} aria-label="Menu"><Menu size={18} /></button>
          <p className="hidden text-sm font-medium text-ink-500 lg:block">Every fix is a hand-off. Keep the baton moving.</p>
          <Bell_ />
        </header>
        {!user.emailVerified && <VerifyBanner email={user.email} />}
        {user.role === 'CONTRACTOR' && !user.approved && (
          <div className="border-b border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800 lg:px-8">
            Your contractor account is awaiting admin approval. You can sign in, but you won't receive work orders until you're approved.
          </div>
        )}
        <main className="mx-auto max-w-6xl px-4 py-6 lg:px-8 lg:py-8"><Outlet /></main>
      </div>
    </div>
  )
}
