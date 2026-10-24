import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import api, { errorMessage } from '../api'
import Logo from '../components/Logo'
import PasswordInput from '../components/PasswordInput'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const nav = useNavigate()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (password !== confirm) return setError('The two passwords do not match')
    setBusy(true); setError('')
    try {
      await api.post('/auth/reset-password', { token, password })
      nav('/login', { replace: true, state: { notice: 'Password updated. Sign in with your new password.' } })
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-md">
        <div className="mb-8"><Logo /></div>
        {!token ? (
          <>
            <h1 className="text-3xl font-extrabold">Link not valid</h1>
            <p className="mt-2 text-ink-500">This reset link is missing its token. Request a new one.</p>
            <Link to="/forgot-password" className="btn-primary mt-6 inline-flex">Request a new link</Link>
          </>
        ) : (
          <>
            <h1 className="text-3xl font-extrabold">Set a new password</h1>
            <p className="mt-1 text-ink-500">Use at least 8 characters. You'll be signed out everywhere else.</p>
            <form onSubmit={submit} className="mt-6 space-y-4">
              <div><label className="label">New password</label><PasswordInput required minLength={8} autoFocus autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} /></div>
              <div><label className="label">Confirm password</label><PasswordInput required minLength={8} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} /></div>
              {error && (
                <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700">
                  {error} {/expired|invalid/.test(error) && <Link to="/forgot-password" className="font-bold underline">Request a new link</Link>}
                </p>
              )}
              <button className="btn-primary w-full" disabled={busy}>{busy ? 'Saving…' : 'Update password'}<ArrowRight size={16} /></button>
            </form>
          </>
        )}
      </div>
    </div>
  )
}
