import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import PasswordInput from '../components/PasswordInput'
import { ArrowRight } from 'lucide-react'
import { errorMessage } from '../api'
import { useAuth } from '../auth'
import Logo, { Baton } from '../components/Logo'

export default function Login() {
  const { user, login, register } = useAuth()
  const nav = useNavigate()
  const notice = useLocation().state?.notice
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '', role: 'CITIZEN' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      if (mode === 'login') await login(form.email, form.password)
      else await register(form)
      nav('/')
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <section className="relative hidden overflow-hidden bg-ink p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <Logo dark />
        <div className="relative z-10 max-w-lg">
          <p className="mb-4 inline-block rounded-full bg-white/10 px-3 py-1 text-xs font-semibold tracking-wide text-baton">WORK ORDERS · SLAs · PAYMENTS</p>
          <h1 className="font-display text-6xl font-extrabold leading-[1.02]">Every fix is a <span className="text-baton">hand-off.</span></h1>
          <p className="mt-5 max-w-md text-lg text-ink-300">Citizens report. Admins assign. Contractors deliver. Relay keeps the baton moving, tracks the clock and settles the payment.</p>
        </div>
        <div className="relative z-10">
          <div className="relative h-14 overflow-hidden">
            <div className="absolute left-0 right-0 top-1/2 h-1 -translate-y-1/2 rounded-full bg-white/10" />
            {[0, 25, 50, 75, 100].map((p) => <span key={p} className="absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white/30 bg-ink" style={{ left: `${p}%` }} />)}
            <div className="baton-run absolute left-0 top-1/2 -mt-5"><Baton size={40} /></div>
          </div>
          <div className="mt-2 flex justify-between text-xs font-medium text-ink-300">
            <span>Reported</span><span>Assigned</span><span>In progress</span><span>Resolved</span><span>Paid</span>
          </div>
        </div>
        <div className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-baton/20 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-40 left-20 h-96 w-96 rounded-full bg-violet-500/20 blur-3xl" />
      </section>

      <section className="flex items-center justify-center p-6">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden"><Logo /></div>
          <h2 className="text-3xl font-extrabold">{mode === 'login' ? 'Welcome back' : 'Join the relay'}</h2>
          <p className="mt-1 text-ink-500">{mode === 'login' ? 'Sign in to pick up where you left off.' : 'Create an account to report or deliver fixes.'}</p>

          <form onSubmit={submit} className="mt-6 space-y-4">
            {mode === 'register' && (
              <>
                <div><label className="label">Full name</label><input className="input" required value={form.name} onChange={set('name')} /></div>
                <div>
                  <label className="label">I am a</label>
                  <div className="grid grid-cols-2 gap-2">
                    {['CITIZEN', 'CONTRACTOR'].map((r) => (
                      <button type="button" key={r} onClick={() => setForm({ ...form, role: r })}
                        className={`rounded-xl border px-3 py-2.5 text-sm font-semibold ${form.role === r ? 'border-baton bg-baton-50 text-baton-600' : 'border-ink/15 bg-white'}`}>
                        {r === 'CITIZEN' ? 'Citizen' : 'Contractor'}
                      </button>
                    ))}
                  </div>
                  {form.role === 'CONTRACTOR' && <p className="mt-2 text-xs text-ink-500">Contractor accounts are reviewed by a Relay admin before you can receive work orders.</p>}
                </div>
              </>
            )}
            <div><label className="label">Email</label><input className="input" type="email" required value={form.email} onChange={set('email')} /></div>
            <div>
              <div className="flex items-baseline justify-between">
                <label className="label">Password</label>
                {mode === 'login' && <Link to="/forgot-password" className="mb-1.5 text-xs font-bold text-baton-600 hover:underline">Forgot password?</Link>}
              </div>
              <PasswordInput required minLength={mode === 'register' ? 8 : undefined} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={form.password} onChange={set('password')} />
            </div>
            {notice && !error && <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700">{notice}</p>}
            {error && <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700">{error}</p>}
            <button className="btn-primary w-full" disabled={busy}>{busy ? 'One moment…' : mode === 'login' ? 'Sign in' : 'Create account'}<ArrowRight size={16} /></button>
          </form>

          <p className="mt-6 text-center text-sm text-ink-500">
            {mode === 'login' ? 'New to Relay?' : 'Already have an account?'}{' '}
            <button className="font-bold text-baton-600 hover:underline" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>
              {mode === 'login' ? 'Create an account' : 'Sign in'}
            </button>
          </p>
        </div>
      </section>
    </div>
  )
}
