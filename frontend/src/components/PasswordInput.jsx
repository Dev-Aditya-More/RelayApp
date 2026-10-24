import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

/** Password field with a show/hide toggle. Accepts the usual input props. */
export default function PasswordInput({ className = '', ...props }) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative">
      <input {...props} type={show ? 'text' : 'password'} className={`input !pr-11 ${className}`} />
      <button type="button" onClick={() => setShow((s) => !s)} tabIndex={-1}
        aria-label={show ? 'Hide password' : 'Show password'} aria-pressed={show}
        className="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-lg p-2 text-ink-500 hover:bg-paper-200 hover:text-ink">
        {show ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </div>
  )
}
