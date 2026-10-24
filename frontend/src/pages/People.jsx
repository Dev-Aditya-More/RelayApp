import { useEffect, useMemo, useState } from 'react'
import { Check, Copy, ShieldCheck, ShieldOff, UserCheck } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import { Spinner } from '../components/Bits'
import { useToast } from '../components/Toast'
import { ROLE_LABEL, timeAgo } from '../utils'

const FILTERS = [
  { key: 'ALL', label: 'Everyone' },
  { key: 'PENDING', label: 'Awaiting approval' },
  { key: 'CONTRACTOR', label: 'Contractors' },
  { key: 'CITIZEN', label: 'Citizens' },
  { key: 'ADMIN', label: 'Admins' },
]

function DemoAccounts() {
  const toast = useToast()
  const [rows, setRows] = useState(null)
  useEffect(() => { api.get('/admin/demo-accounts').then((r) => setRows(r.data)).catch(() => setRows([])) }, [])
  if (!rows || rows.length === 0) return null

  const copy = (text) => navigator.clipboard?.writeText(text).then(() => toast('Copied'))
  return (
    <div className="card mb-6 p-5">
      <h2 className="font-display text-lg font-extrabold">Demo logins</h2>
      <p className="mb-3 text-sm text-ink-500">Only admins see these. Use them to walk a prospect through Relay without touching real accounts.</p>
      <div className="grid gap-2 sm:grid-cols-2">
        {rows.map((d) => (
          <div key={d.email} className="flex items-center justify-between gap-3 rounded-xl bg-paper-200 px-3 py-2.5 text-sm">
            <div className="min-w-0"><p className="truncate font-bold">{d.name} <span className="font-medium text-ink-500">· {ROLE_LABEL[d.role]}</span></p><p className="truncate text-xs text-ink-500">{d.email}</p></div>
            <button className="btn-ghost !px-2.5 !py-1.5 text-xs" onClick={() => copy(`${d.email} / ${d.password}`)}><Copy size={13} />Copy</button>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function People() {
  const { user: me } = useAuth()
  const toast = useToast()
  const [users, setUsers] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [q, setQ] = useState('')

  useEffect(() => { api.get('/admin/users').then((r) => setUsers(r.data)) }, [])

  const patch = async (u, body, done) => {
    try {
      const { data } = await api.put(`/admin/users/${u.id}`, body)
      setUsers((list) => list.map((x) => (x.id === data.id ? data : x)))
      toast(done)
    } catch (e) { toast(errorMessage(e), 'err') }
  }

  const shown = useMemo(() => (users || []).filter((u) => {
    if (filter === 'PENDING' ? !(u.role === 'CONTRACTOR' && !u.approved) : filter !== 'ALL' && u.role !== filter) return false
    const t = q.trim().toLowerCase()
    return !t || u.name.toLowerCase().includes(t) || u.email.toLowerCase().includes(t)
  }), [users, filter, q])

  if (!users) return <Spinner />
  const pending = users.filter((u) => u.role === 'CONTRACTOR' && !u.approved).length

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold sm:text-4xl">People</h1>
      <p className="mb-6 mt-1 max-w-2xl text-ink-500">Approve contractors before they can receive work, suspend accounts, and make teammates admins.</p>

      <DemoAccounts />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        {FILTERS.map((f) => (
          <button key={f.key} onClick={() => setFilter(f.key)}
            className={`rounded-full border px-3.5 py-1.5 text-xs font-bold ${filter === f.key ? 'border-baton bg-baton-50 text-baton-600' : 'border-ink/15 bg-white text-ink-500'}`}>
            {f.label}{f.key === 'PENDING' && pending > 0 && <span className="ml-1.5 rounded-full bg-baton px-1.5 text-white">{pending}</span>}
          </button>
        ))}
        <input className="input !w-56 !py-1.5 sm:ml-auto" placeholder="Search name or email" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>

      <div className="card overflow-x-auto">
        <table className="w-full min-w-[720px] text-sm">
          <thead><tr className="border-b border-ink/10 text-left text-xs uppercase tracking-wide text-ink-500"><th className="p-4">Person</th><th>Role</th><th>Status</th><th>Joined</th><th /></tr></thead>
          <tbody className="divide-y divide-ink/10">
            {shown.length === 0 && <tr><td colSpan={5} className="p-8 text-center text-ink-500">No one matches.</td></tr>}
            {shown.map((u) => {
              const self = u.id === me.id
              return (
                <tr key={u.id}>
                  <td className="p-4"><p className="font-bold">{u.name}{self && <span className="ml-2 text-xs font-medium text-ink-500">(you)</span>}</p><p className="text-xs text-ink-500">{u.email}</p></td>
                  <td className="font-semibold">{ROLE_LABEL[u.role]}</td>
                  <td>
                    {u.suspended ? <span className="chip bg-rose-100 text-rose-700">Suspended</span>
                      : u.role === 'CONTRACTOR' && !u.approved ? <span className="chip bg-amber-100 text-amber-700">Awaiting approval</span>
                      : <span className="chip bg-emerald-100 text-emerald-700">Active</span>}
                  </td>
                  <td className="text-ink-500">{timeAgo(u.createdAt)}</td>
                  <td className="p-4">
                    {!self && (
                      <div className="flex flex-wrap justify-end gap-2">
                        {u.role === 'CONTRACTOR' && !u.approved && (
                          <button className="btn-primary !px-3 !py-1.5 text-xs" onClick={() => patch(u, { approved: true }, `${u.name} approved`)}><Check size={14} />Approve</button>
                        )}
                        {u.role !== 'ADMIN' && (
                          <button className="btn-ghost !px-3 !py-1.5 text-xs"
                            onClick={() => window.confirm(`Make ${u.name} an admin? They will have full control of Relay.`) && patch(u, { role: 'ADMIN' }, `${u.name} is now an admin`)}>
                            <ShieldCheck size={14} />Make admin
                          </button>
                        )}
                        {u.role === 'ADMIN' && (
                          <button className="btn-ghost !px-3 !py-1.5 text-xs" onClick={() => patch(u, { role: 'CITIZEN' }, `${u.name} is no longer an admin`)}><UserCheck size={14} />Remove admin</button>
                        )}
                        <button className="btn-ghost !px-3 !py-1.5 text-xs" onClick={() => patch(u, { suspended: !u.suspended }, u.suspended ? `${u.name} restored` : `${u.name} suspended`)}>
                          <ShieldOff size={14} />{u.suspended ? 'Restore' : 'Suspend'}
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
