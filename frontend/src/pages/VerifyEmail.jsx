import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CheckCircle2, XCircle } from 'lucide-react'
import api, { errorMessage } from '../api'
import { useAuth } from '../auth'
import Logo from '../components/Logo'
import { Spinner } from '../components/Bits'

export default function VerifyEmail() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const { user, refresh } = useAuth()
  const started = useRef(false)
  const [state, setState] = useState(token ? 'working' : 'error')
  const [error, setError] = useState(token ? '' : 'This verification link is missing its token.')

  useEffect(() => {
    // StrictMode runs effects twice in dev; the token is single-use, so only call once.
    if (!token || started.current) return
    started.current = true
    api.post('/auth/verify-email', { token })
      .then(() => { setState('done'); if (localStorage.getItem('relay.token')) refresh() })
      .catch((e) => { setError(errorMessage(e)); setState('error') })
  }, [token, refresh])

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-md">
        <div className="mb-8"><Logo /></div>
        {state === 'working' && <Spinner />}
        {state === 'done' && (
          <div>
            <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-emerald-100 text-emerald-700"><CheckCircle2 /></span>
            <h1 className="text-3xl font-extrabold">Email verified</h1>
            <p className="mt-2 text-ink-500">You're all set. You can now report issues and receive work orders.</p>
            <Link to={user ? '/' : '/login'} className="btn-primary mt-6 inline-flex">{user ? 'Go to Relay' : 'Sign in'}</Link>
          </div>
        )}
        {state === 'error' && (
          <div>
            <span className="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-rose-100 text-rose-700"><XCircle /></span>
            <h1 className="text-3xl font-extrabold">Couldn't verify</h1>
            <p className="mt-2 text-ink-500">{error}</p>
            <Link to={user ? '/' : '/login'} className="btn-ghost mt-6 inline-flex">{user ? 'Go to Relay' : 'Back to sign in'}</Link>
          </div>
        )}
      </div>
    </div>
  )
}
