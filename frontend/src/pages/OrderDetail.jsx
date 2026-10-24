import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Banknote, Camera, CheckCheck, Gavel, MapPin, PlayCircle, UserCheck, Zap } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import { CategoryIcon, FastTrackTag, RiskMeter, SeverityTag, SlaTimer, Spinner, Stars, StatusBadge } from '../components/Bits'
import PayModal from '../components/PayModal'
import RelayTrack from '../components/RelayTrack'
import { useToast } from '../components/Toast'
import { CATEGORY, PAYMENT_TYPE, fmtDate, inr } from '../utils'

const KIND_DOT = {
  REPORTED: 'bg-ink', ASSIGNED: 'bg-violet-500', IN_PROGRESS: 'bg-sky-500', COMPLETED: 'bg-emerald-500', CLOSED: 'bg-slate-500',
  FAST_TRACK: 'bg-baton', SLA_BREACH: 'bg-rose-500', FINE: 'bg-rose-500', FINE_PAID: 'bg-emerald-500', PAYOUT: 'bg-emerald-500',
}

function Fact({ label, children }) {
  return <div><dt className="text-xs font-semibold uppercase tracking-wide text-ink-500">{label}</dt><dd className="mt-0.5 text-sm font-bold">{children}</dd></div>
}

function StatusForm({ order, onDone }) {
  const toast = useToast()
  const [note, setNote] = useState('')
  const [photo, setPhoto] = useState(null)
  const [busy, setBusy] = useState(false)
  const file = useRef(null)
  const target = order.status === 'ASSIGNED' ? 'IN_PROGRESS' : 'COMPLETED'

  const submit = async () => {
    setBusy(true)
    const body = new FormData()
    body.append('status', target)
    if (note) body.append('note', note)
    if (photo) body.append('photo', photo)
    try { await api.post(`/work-orders/${order.id}/status`, body); toast(target === 'COMPLETED' ? 'Marked complete. Nice work.' : 'Work started'); onDone() }
    catch (e) { toast(errorMessage(e), 'err'); setBusy(false) }
  }

  return (
    <div className="space-y-3">
      <textarea className="input min-h-20" placeholder={target === 'COMPLETED' ? 'What was done? (visible to the citizen)' : 'Crew details, ETA…'} value={note} onChange={(e) => setNote(e.target.value)} />
      <input ref={file} type="file" accept="image/*" hidden onChange={(e) => setPhoto(e.target.files[0])} />
      <button className="btn-ghost w-full" onClick={() => file.current.click()}><Camera size={16} />{photo ? photo.name : target === 'COMPLETED' ? 'Add completion photo' : 'Add photo (optional)'}</button>
      <button className="btn-primary w-full" disabled={busy} onClick={submit}>
        {target === 'COMPLETED' ? <><CheckCheck size={16} />Mark as complete</> : <><PlayCircle size={16} />Start work</>}
      </button>
    </div>
  )
}

export default function OrderDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [contractors, setContractors] = useState([])
  const [pick, setPick] = useState('')
  const [rating, setRating] = useState(0)
  const [feedback, setFeedback] = useState('')
  const [checkout, setCheckout] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(() =>
    api.get(`/work-orders/${id}`).then((r) => setData(r.data)).catch((e) => setError(errorMessage(e))), [id])

  useEffect(() => { load() }, [load])
  useEffect(() => { if (user.role === 'ADMIN') api.get('/contractors').then((r) => setContractors(r.data)) }, [user.role])

  const act = async (fn, okMsg) => {
    setBusy(true)
    try { await fn(); toast(okMsg); await load() } catch (e) { toast(errorMessage(e), 'err') } finally { setBusy(false) }
  }

  if (error) return <div className="card p-8 text-center"><p className="font-bold">{error}</p><Link to="/orders" className="btn-ghost mt-4">Back to list</Link></div>
  if (!data) return <Spinner />

  const { order: o, events, payments } = data
  const active = !o.completedAt
  const isAdmin = user.role === 'ADMIN'
  const isContractor = user.role === 'CONTRACTOR'
  const isCitizen = user.role === 'CITIZEN'
  const finePayment = payments.find((p) => p.type === 'FINE' && p.status !== 'PAID')

  const startFastTrack = async () => {
    try { const { data: c } = await api.post(`/work-orders/${o.id}/fast-track`); setCheckout(c) } catch (e) { toast(errorMessage(e), 'err') }
  }
  const startFinePay = async () => {
    try { const { data: c } = await api.post(`/payments/${finePayment.id}/checkout`); setCheckout(c) } catch (e) { toast(errorMessage(e), 'err') }
  }

  return (
    <div>
      <Link to="/orders" className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-ink-500 hover:text-ink"><ArrowLeft size={16} />Back</Link>

      <div className="card mb-6 p-5 sm:p-6">
        <div className="flex flex-wrap items-start gap-4">
          <CategoryIcon category={o.category} size={24} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2"><span className="font-mono text-sm font-bold text-ink-500">{o.code}</span><StatusBadge status={o.status} />{o.fastTrack && <FastTrackTag />}{o.escalated && <span className="chip bg-rose-100 text-rose-700">Escalated</span>}</div>
            <h1 className="mt-1 font-display text-2xl font-extrabold sm:text-3xl">{o.title}</h1>
            <p className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink-500">
              <span className="inline-flex items-center gap-1"><MapPin size={14} />{[o.area, o.address].filter(Boolean).join(' · ')}</span>
              <span>{CATEGORY[o.category].label}</span><SeverityTag severity={o.severity} />
            </p>
          </div>
          <div className="flex flex-col items-end gap-2"><SlaTimer order={o} />{active && <RiskMeter value={o.delayRisk} />}</div>
        </div>
        <div className="mt-6"><RelayTrack status={o.status} breached={o.slaBreached} /></div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="card p-5 sm:p-6">
            <h3 className="mb-2 font-display text-lg font-bold">What was reported</h3>
            <p className="whitespace-pre-line text-sm leading-relaxed text-ink-700">{o.description}</p>
            {o.photoUrl && <img src={o.photoUrl} alt="Reported issue" className="mt-4 max-h-80 w-full rounded-xl object-cover" />}
            <dl className="mt-5 grid grid-cols-2 gap-4 border-t border-ink/10 pt-5 sm:grid-cols-4">
              <Fact label="Department">{o.department}</Fact>
              <Fact label="SLA window">{o.slaHours}h{o.fastTrack ? ' (fast-track)' : ''}</Fact>
              <Fact label="Deadline">{fmtDate(o.slaDeadline)}</Fact>
              <Fact label="Reporter">{o.citizenName}</Fact>
              <Fact label="Carried by">{o.contractor?.name ?? 'Not assigned yet'}</Fact>
              <Fact label="Reported">{fmtDate(o.createdAt)}</Fact>
              <Fact label="Resolved">{fmtDate(o.completedAt)}</Fact>
              {o.latitude && <Fact label="GPS">{o.latitude.toFixed(4)}, {o.longitude.toFixed(4)}</Fact>}
            </dl>
          </div>

          {o.rating && (
            <div className="card p-5 sm:p-6">
              <h3 className="mb-2 font-display text-lg font-bold">Citizen feedback</h3>
              <div className="flex items-center gap-3"><Stars value={o.rating} size={20} /><span className={`chip ${o.sentiment === 'POSITIVE' ? 'bg-emerald-100 text-emerald-700' : o.sentiment === 'NEGATIVE' ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-700'}`}>{o.sentiment?.toLowerCase()} sentiment</span></div>
              {o.feedback && <p className="mt-2 text-sm text-ink-700">“{o.feedback}”</p>}
            </div>
          )}

          <div className="card p-5 sm:p-6">
            <h3 className="mb-4 font-display text-lg font-bold">Hand-off timeline</h3>
            <ol className="relative space-y-5 border-l-2 border-ink/10 pl-6">
              {events.map((e, i) => (
                <li key={i} className="relative">
                  <span className={`absolute -left-[31px] top-1 h-3 w-3 rounded-full ring-4 ring-white ${KIND_DOT[e.kind] ?? 'bg-ink'}`} />
                  <p className="text-sm font-semibold">{e.message}</p>
                  <p className="text-xs text-ink-500">{e.actor} · {fmtDate(e.createdAt)}</p>
                  {e.photoUrl && <img src={e.photoUrl} alt="" className="mt-2 max-h-48 rounded-lg" />}
                </li>
              ))}
            </ol>
          </div>
        </div>

        <aside className="space-y-6">
          {isAdmin && (o.status === 'OPEN' || o.status === 'ASSIGNED') && (
            <div className="card p-5">
              <h3 className="mb-3 flex items-center gap-2 font-display text-lg font-bold"><UserCheck size={18} className="text-baton" />{o.contractor ? 'Reassign' : 'Hand off to a contractor'}</h3>
              <select className="input mb-3" value={pick} onChange={(e) => setPick(e.target.value)}>
                <option value="">Choose contractor…</option>
                {contractors.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <button className="btn-primary w-full" disabled={!pick || busy} onClick={() => act(() => api.post(`/work-orders/${o.id}/assign`, { contractorId: Number(pick) }), 'Baton handed over')}>Assign</button>
            </div>
          )}

          {isContractor && (o.status === 'ASSIGNED' || o.status === 'IN_PROGRESS') && (
            <div className="card p-5"><h3 className="mb-3 font-display text-lg font-bold">Your move</h3><StatusForm order={o} onDone={load} /></div>
          )}

          {isCitizen && active && !o.fastTrack && (
            <div className="card overflow-hidden">
              <div className="bg-ink p-5 text-white">
                <p className="flex items-center gap-2 font-display text-lg font-bold"><Zap size={18} className="text-baton" />Fast-track this fix</p>
                <p className="mt-1 text-sm text-ink-300">Priority handling and half the SLA window.</p>
              </div>
              <div className="p-5"><button className="btn-primary w-full" onClick={startFastTrack}>Pay {inr(o.fastTrackFee)} to fast-track</button></div>
            </div>
          )}

          {isCitizen && o.status === 'COMPLETED' && (
            <div className="card p-5">
              <h3 className="mb-1 font-display text-lg font-bold">How did we do?</h3>
              <p className="mb-3 text-sm text-ink-500">Your rating closes this work order.</p>
              <Stars value={rating} onChange={setRating} />
              <textarea className="input mt-3 min-h-20" placeholder="Anything to add? (optional)" value={feedback} onChange={(e) => setFeedback(e.target.value)} />
              <button className="btn-primary mt-3 w-full" disabled={!rating || busy} onClick={() => act(() => api.post(`/work-orders/${o.id}/rate`, { rating, feedback }), 'Thanks for the feedback')}>Submit rating</button>
            </div>
          )}

          {isAdmin && o.contractor && !o.contractorPaid && o.completedAt && (
            <div className="card p-5">
              <h3 className="mb-1 flex items-center gap-2 font-display text-lg font-bold"><Banknote size={18} className="text-baton" />Contractor payment</h3>
              <p className="mb-3 text-sm text-ink-500">Work is complete. Release {inr(o.payoutAmount)} to {o.contractor.name}.</p>
              <button className="btn-ink w-full" disabled={busy} onClick={() => act(() => api.post(`/work-orders/${o.id}/release-payment`), 'Payment released')}>Release {inr(o.payoutAmount)}</button>
            </div>
          )}

          {isAdmin && o.slaBreached && o.contractor && !o.fineIssued && (
            <div className="card border-rose-200 p-5">
              <h3 className="mb-1 flex items-center gap-2 font-display text-lg font-bold text-rose-700"><Gavel size={18} />SLA breached</h3>
              <p className="mb-3 text-sm text-ink-500">Issue the {inr(o.fineAmount)} penalty to {o.contractor.name}.</p>
              <button className="btn-danger w-full" disabled={busy} onClick={() => act(() => api.post(`/work-orders/${o.id}/fine`), 'Penalty issued')}>Issue {inr(o.fineAmount)} penalty</button>
            </div>
          )}

          {isAdmin && o.status === 'COMPLETED' && (
            <button className="btn-ghost w-full" disabled={busy} onClick={() => act(() => { const b = new FormData(); b.append('status', 'CLOSED'); return api.post(`/work-orders/${o.id}/status`, b) }, 'Work order closed')}>Close work order</button>
          )}

          {isContractor && finePayment && (
            <div className="card border-rose-200 p-5">
              <h3 className="mb-1 font-display text-lg font-bold text-rose-700">Penalty due</h3>
              <p className="mb-3 text-sm text-ink-500">{finePayment.note}</p>
              <button className="btn-danger w-full" onClick={startFinePay}>Pay {inr(finePayment.amount)}</button>
            </div>
          )}

          {payments.length > 0 && (
            <div className="card p-5">
              <h3 className="mb-3 font-display text-lg font-bold">Payments</h3>
              <ul className="divide-y divide-ink/10 text-sm">
                {payments.map((p) => (
                  <li key={p.id} className="flex items-center justify-between py-2.5">
                    <span><b>{PAYMENT_TYPE[p.type]}</b><br /><span className="text-xs text-ink-500">{p.status === 'PAID' ? `Paid ${fmtDate(p.paidAt)}` : 'Awaiting payment'}</span></span>
                    <span className="font-bold">{inr(p.amount)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </aside>
      </div>

      {checkout && <PayModal checkout={checkout} user={user} onClose={() => setCheckout(null)}
        onPaid={() => { setCheckout(null); toast('Payment received'); load() }} />}
    </div>
  )
}
