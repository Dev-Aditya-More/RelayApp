import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FileText, Printer } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import { Empty, Modal, Spinner } from '../components/Bits'
import { Baton } from '../components/Logo'
import PayModal from '../components/PayModal'
import { useToast } from '../components/Toast'
import { PAYMENT_TYPE, fmtDate, inr } from '../utils'

const STATUS_CLS = { PAID: 'bg-emerald-100 text-emerald-700', PENDING: 'bg-amber-100 text-amber-800', CREATED: 'bg-amber-100 text-amber-800', FAILED: 'bg-rose-100 text-rose-700' }

function Invoice({ p, onClose }) {
  return (
    <Modal title="Invoice" onClose={onClose} wide>
      <div className="print-area rounded-xl bg-white p-2">
        <div className="flex items-start justify-between border-b border-ink/10 pb-4">
          <div className="flex items-center gap-2"><span className="grid h-10 w-10 place-items-center rounded-xl bg-ink"><Baton size={30} /></span><span className="font-display text-2xl font-extrabold">relay<span className="text-baton">.</span></span></div>
          <div className="text-right text-sm"><p className="font-mono font-bold">{p.invoiceNo}</p><p className="text-ink-500">{fmtDate(p.paidAt)}</p></div>
        </div>
        <div className="grid grid-cols-2 gap-4 py-4 text-sm">
          <div><p className="label">From</p><p className="font-bold">{p.payerName}</p></div>
          <div><p className="label">To</p><p className="font-bold">{p.payeeName ?? 'Relay Services'}</p></div>
        </div>
        <table className="w-full text-sm">
          <thead><tr className="border-y border-ink/10 text-left text-xs uppercase tracking-wide text-ink-500"><th className="py-2">Description</th><th className="text-right">Amount</th></tr></thead>
          <tbody><tr><td className="py-3"><b>{PAYMENT_TYPE[p.type]}</b><br /><span className="text-ink-500">{p.workOrderCode} · {p.workOrderTitle}</span></td><td className="text-right font-bold">{inr(p.amount)}</td></tr></tbody>
          <tfoot><tr className="border-t border-ink/10"><td className="py-3 font-bold">Total paid</td><td className="text-right font-display text-xl font-extrabold">{inr(p.amount)}</td></tr></tfoot>
        </table>
        <p className="mt-2 text-xs text-ink-500">Reference: {p.razorpayPaymentId}</p>
      </div>
      <button className="btn-ink mt-4" onClick={() => window.print()}><Printer size={16} />Print / save PDF</button>
    </Modal>
  )
}

export default function Payments() {
  const { user } = useAuth()
  const toast = useToast()
  const [rows, setRows] = useState(null)
  const [invoice, setInvoice] = useState(null)
  const [checkout, setCheckout] = useState(null)

  const load = useCallback(() => api.get('/payments').then((r) => setRows(r.data)), [])
  useEffect(() => { load() }, [load])
  if (!rows) return <Spinner />

  const pay = async (p) => {
    try { const { data } = await api.post(`/payments/${p.id}/checkout`); setCheckout(data) } catch (e) { toast(errorMessage(e), 'err') }
  }
  const total = (t) => rows.filter((p) => p.status === 'PAID' && p.type === t).reduce((s, p) => s + p.amount, 0)

  return (
    <div>
      <h1 className="mb-6 font-display text-3xl font-extrabold sm:text-4xl">Payments</h1>
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        {Object.entries(PAYMENT_TYPE).map(([k, label]) => (
          <div key={k} className="card p-4"><p className="text-xs font-semibold uppercase tracking-wide text-ink-500">{label}</p><p className="mt-1 font-display text-2xl font-extrabold">{inr(total(k))}</p></div>
        ))}
      </div>

      {rows.length === 0 ? <Empty title="No payments yet" hint="Fast-track fees, penalties and contractor payouts will be listed here." /> : (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[720px] text-sm">
            <thead><tr className="border-b border-ink/10 text-left text-xs uppercase tracking-wide text-ink-500"><th className="p-4">Type</th><th>Work order</th><th>From → To</th><th>Amount</th><th>Status</th><th /></tr></thead>
            <tbody className="divide-y divide-ink/10">
              {rows.map((p) => (
                <tr key={p.id} className="hover:bg-paper/60">
                  <td className="p-4 font-bold">{PAYMENT_TYPE[p.type]}<br /><span className="text-xs font-normal text-ink-500">{fmtDate(p.paidAt ?? p.createdAt)}</span></td>
                  <td><Link className="font-mono text-xs font-bold text-baton-600 hover:underline" to={`/orders/${p.workOrderId}`}>{p.workOrderCode}</Link></td>
                  <td className="text-ink-700">{p.payerName} → {p.payeeName ?? 'Relay'}</td>
                  <td className="font-bold">{inr(p.amount)}</td>
                  <td><span className={`chip ${STATUS_CLS[p.status]}`}>{p.status === 'CREATED' ? 'PENDING' : p.status}</span></td>
                  <td className="pr-4 text-right">
                    {p.status === 'PAID'
                      ? <button className="btn-ghost !px-3 !py-1.5 text-xs" onClick={() => api.get(`/payments/${p.id}/invoice`).then((r) => setInvoice(r.data))}><FileText size={14} />Invoice</button>
                      : p.payerName === user.name && <button className="btn-primary !px-3 !py-1.5 text-xs" onClick={() => pay(p)}>Pay now</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {invoice && <Invoice p={invoice} onClose={() => setInvoice(null)} />}
      {checkout && <PayModal checkout={checkout} user={user} onClose={() => setCheckout(null)}
        onPaid={() => { setCheckout(null); toast('Payment received'); load() }} />}
    </div>
  )
}
