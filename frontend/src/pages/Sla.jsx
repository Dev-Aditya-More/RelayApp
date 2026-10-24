import { useEffect, useState } from 'react'
import { Check, Pencil } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import { CategoryIcon, Spinner } from '../components/Bits'
import { useToast } from '../components/Toast'
import { CATEGORY, inr } from '../utils'

function Row({ p, editable, onSaved }) {
  const toast = useToast()
  const [edit, setEdit] = useState(false)
  const [f, setF] = useState(p)
  const num = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const save = async () => {
    try {
      const { data } = await api.put(`/sla-policies/${p.id}`, {
        hours: Number(f.hours), fineAmount: Number(f.fineAmount), contractorRate: Number(f.contractorRate), fastTrackFee: Number(f.fastTrackFee),
      })
      toast(`${CATEGORY[p.category].label} policy updated`)
      setEdit(false); onSaved(data)
    } catch (e) { toast(errorMessage(e), 'err') }
  }

  const cell = (k, fmt) => edit
    ? <input type="number" min="1" className="input !w-24 !py-1.5" value={f[k]} onChange={num(k)} />
    : <span className="font-bold">{fmt(p[k])}</span>

  return (
    <tr>
      <td className="p-4"><div className="flex items-center gap-3"><CategoryIcon category={p.category} size={16} /><div><p className="font-bold">{CATEGORY[p.category].label}</p><p className="text-xs text-ink-500">{p.department}</p></div></div></td>
      <td>{cell('hours', (v) => `${v}h`)}</td>
      <td>{cell('fastTrackFee', inr)}</td>
      <td>{cell('fineAmount', inr)}</td>
      <td>{cell('contractorRate', inr)}</td>
      {editable && <td className="pr-4 text-right">{edit
        ? <button className="btn-primary !px-3 !py-1.5 text-xs" onClick={save}><Check size={14} />Save</button>
        : <button className="btn-ghost !px-3 !py-1.5 text-xs" onClick={() => { setF(p); setEdit(true) }}><Pencil size={14} />Edit</button>}</td>}
    </tr>
  )
}

export default function Sla() {
  const { user } = useAuth()
  const [rows, setRows] = useState(null)
  useEffect(() => { api.get('/sla-policies').then((r) => setRows(r.data)) }, [])
  if (!rows) return <Spinner />
  const editable = user.role === 'ADMIN'

  return (
    <div>
      <h1 className="font-display text-3xl font-extrabold sm:text-4xl">SLA policies</h1>
      <p className="mb-6 mt-1 max-w-2xl text-ink-500">Each issue type has a resolution window. Miss it and the work order is flagged, escalated and open to a penalty. Changes apply to new reports.</p>
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[680px] text-sm">
          <thead><tr className="border-b border-ink/10 text-left text-xs uppercase tracking-wide text-ink-500"><th className="p-4">Issue type</th><th>Resolve within</th><th>Fast-track fee</th><th>Breach penalty</th><th>Contractor rate</th>{editable && <th />}</tr></thead>
          <tbody className="divide-y divide-ink/10">
            {rows.map((p) => <Row key={p.id} p={p} editable={editable} onSaved={(n) => setRows(rows.map((r) => r.id === n.id ? n : r))} />)}
          </tbody>
        </table>
      </div>
    </div>
  )
}
