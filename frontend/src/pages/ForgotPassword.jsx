import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, ArrowRight, MailCheck } from 'lucide-react'
import api, { errorMessage } from '../api'
import Logo from '../components/Logo'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try { await api.post('/auth/forgot-password', { email }); setSent(true) }
    catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-md">
        <div className="mb-8"><Logo /></div>
        {sent ? (
          <div>
            <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-emerald-100 text-emerald-700"><MailCheck /></span>
            <h1 className="text-3xl font-extrabold">Check your email</h1>
            <p className="mt-2 text-ink-500">If an account exists for <b className="text-ink">{email}</b>, a reset link is on its way. It works for 30 minutes. Check spam if you don't see it.</p>
            <button className="btn-ghost mt-6" onClick={() => setSent(false)}>Use a different email</button>
          </div>
        ) : (
          <>
            <h1 className="text-3xl font-extrabold">Forgot your password?</h1>
            <p className="mt-1 text-ink-500">Enter your email and we'll send you a link to set a new one.</p>
            <form onSubmit={submit} className="mt-6 space-y-4">
              <div><label className="label">Email</label><input className="input" type="email" required autoFocus value={email} onChange={(e) => setEmail(e.target.value)} /></div>
              {error && <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700">{error}</p>}
              <button className="btn-primary w-full" disabled={busy}>{busy ? 'Sending…' : 'Send reset link'}<ArrowRight size={16} /></button>
            </form>
          </>
        )}
        <Link to="/login" className="mt-8 inline-flex items-center gap-1.5 text-sm font-bold text-baton-600 hover:underline"><ArrowLeft size={14} />Back to sign in</Link>
      </div>
    </div>
  )
}
