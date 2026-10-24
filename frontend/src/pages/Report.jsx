import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Camera, LocateFixed, Sparkles } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../components/Toast'
import { CATEGORY, SEVERITY } from '../utils'

export default function Report() {
  const { user } = useAuth()
  const toast = useToast()
  const nav = useNavigate()
  const fileInput = useRef(null)
  const [form, setForm] = useState({ title: '', description: '', area: '', address: '', category: '', severity: '', latitude: '', longitude: '' })
  const [photo, setPhoto] = useState(null)
  const [preview, setPreview] = useState(null)
  const [hint, setHint] = useState(null)
  const [busy, setBusy] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  // Relay Assist: re-classify as the citizen types, debounced
  useEffect(() => {
    const text = `${form.title} ${form.description}`.trim()
    if (text.length < 8) { setHint(null); return }
    const id = setTimeout(() => api.post('/work-orders/classify', { text }).then((r) => setHint(r.data)).catch(() => {}), 350)
    return () => clearTimeout(id)
  }, [form.title, form.description])

  if (user.role !== 'CITIZEN') return <Navigate to="/" replace />

  const pickPhoto = (e) => {
    const f = e.target.files[0]
    if (!f) return
    setPhoto(f)
    setPreview(URL.createObjectURL(f))
  }

  const locate = () => {
    if (!navigator.geolocation) return toast('Location is not available in this browser', 'err')
    navigator.geolocation.getCurrentPosition(
      (p) => setForm((f) => ({ ...f, latitude: p.coords.latitude.toFixed(5), longitude: p.coords.longitude.toFixed(5) })),
      () => toast('Could not read your location. You can still submit without it.', 'err'),
    )
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    const body = new FormData()
    Object.entries(form).forEach(([k, v]) => v !== '' && body.append(k, v))
    if (photo) body.append('photo', photo)
    try {
      const { data } = await api.post('/work-orders', body)
      toast(`Report ${data.code} is on its way`)
      nav(`/orders/${data.id}`)
    } catch (err) { toast(errorMessage(err), 'err'); setBusy(false) }
  }

  const effCategory = form.category || hint?.category
  const effSeverity = form.severity || hint?.severity

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-3xl font-extrabold sm:text-4xl">Pass it on.</h1>
      <p className="mb-6 mt-1 text-ink-500">Tell us what is broken. We will log it, start the SLA clock and hand it to the right crew.</p>

      <form onSubmit={submit} className="card space-y-5 p-6">
        <div><label className="label">What is the issue?</label><input className="input" required maxLength={120} placeholder="e.g. Deep pothole near the school gate" value={form.title} onChange={set('title')} /></div>
        <div><label className="label">Details</label><textarea className="input min-h-28" required maxLength={1900} placeholder="How bad is it, how long has it been like this, is anyone at risk?" value={form.description} onChange={set('description')} /></div>

        {hint && (
          <div className="rise flex items-start gap-3 rounded-xl bg-baton-50 p-3.5 text-sm">
            <Sparkles size={18} className="mt-0.5 shrink-0 text-baton" />
            <div>
              <p className="font-bold">Relay Assist thinks this is <span className="text-baton-600">{CATEGORY[hint.category].label}</span>, {SEVERITY[hint.severity].label.toLowerCase()} severity <span className="font-normal text-ink-500">({hint.confidence}% sure)</span></p>
              <p className="text-xs text-ink-500">We will use this unless you pick something else below.</p>
            </div>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Category</label>
            <select className="input" value={form.category} onChange={set('category')}>
              <option value="">{effCategory ? `Auto: ${CATEGORY[effCategory].label}` : 'Auto-detect'}</option>
              {Object.entries(CATEGORY).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Severity</label>
            <select className="input" value={form.severity} onChange={set('severity')}>
              <option value="">{effSeverity ? `Auto: ${SEVERITY[effSeverity].label}` : 'Auto-detect'}</option>
              {Object.entries(SEVERITY).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
            </select>
          </div>
          <div><label className="label">Area / ward</label><input className="input" required placeholder="e.g. Kothrud" value={form.area} onChange={set('area')} /></div>
          <div><label className="label">Street / landmark</label><input className="input" placeholder="Optional" value={form.address} onChange={set('address')} /></div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className="btn-ghost" onClick={locate}><LocateFixed size={16} />Use my GPS location</button>
          {form.latitude && <span className="chip bg-emerald-100 text-emerald-700">{form.latitude}, {form.longitude}</span>}
        </div>

        <div>
          <label className="label">Photo</label>
          <input ref={fileInput} type="file" accept="image/*" hidden onChange={pickPhoto} />
          {preview ? (
            <div className="relative overflow-hidden rounded-xl">
              <img src={preview} alt="Preview" className="max-h-64 w-full object-cover" />
              <button type="button" className="btn-ink absolute right-3 top-3 !py-1.5 text-xs" onClick={() => { setPhoto(null); setPreview(null) }}>Remove</button>
            </div>
          ) : (
            <button type="button" onClick={() => fileInput.current.click()}
              className="flex w-full flex-col items-center gap-2 rounded-xl border-2 border-dashed border-ink/20 py-8 text-sm font-semibold text-ink-500 transition hover:border-baton hover:bg-baton-50 hover:text-baton-600">
              <Camera size={24} />Add a photo as proof
            </button>
          )}
        </div>

        <button className="btn-primary w-full !py-3" disabled={busy}>{busy ? 'Sending…' : 'Submit report'}</button>
      </form>
    </div>
  )
}
