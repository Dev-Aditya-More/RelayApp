import { useEffect, useRef, useState } from 'react'
import { CreditCard, Landmark, Smartphone, ShieldCheck } from 'lucide-react'
import api, { errorMessage } from '../api'
import { Modal } from './Bits'
import { useToast } from './Toast'
import { inr } from '../utils'

const METHODS = [
  { id: 'upi', label: 'UPI', Icon: Smartphone },
  { id: 'card', label: 'Card', Icon: CreditCard },
  { id: 'netbanking', label: 'Net banking', Icon: Landmark },
]

function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = 'https://checkout.razorpay.com/v1/checkout.js'
    s.onload = resolve
    s.onerror = () => reject(new Error('Could not load Razorpay Checkout'))
    document.body.appendChild(s)
  })
}

/**
 * Live mode: opens Razorpay Checkout and verifies the signature server-side.
 * Mock mode (no keys on the server): a stand-in sheet that settles through the same ledger.
 */
export default function PayModal({ checkout, user, onClose, onPaid }) {
  const toast = useToast()
  const [method, setMethod] = useState('upi')
  const [busy, setBusy] = useState(false)
  const opened = useRef(false)

  useEffect(() => {
    if (checkout.mock || opened.current) return
    opened.current = true
    loadRazorpay()
      .then(() => {
        const rzp = new window.Razorpay({
          key: checkout.keyId,
          amount: checkout.amountPaise,
          currency: checkout.currency,
          order_id: checkout.orderId,
          name: 'Relay',
          description: checkout.description,
          prefill: { name: user?.name, email: user?.email },
          theme: { color: '#FF5B2E' },
          handler: async (r) => {
            try {
              await api.post('/payments/verify', {
                razorpayOrderId: r.razorpay_order_id,
                razorpayPaymentId: r.razorpay_payment_id,
                razorpaySignature: r.razorpay_signature,
              })
              onPaid()
            } catch (e) { toast(errorMessage(e), 'err'); onClose() }
          },
          modal: { ondismiss: onClose },
        })
        rzp.open()
      })
      .catch((e) => { toast(e.message, 'err'); onClose() })
  }, [checkout, user, onClose, onPaid, toast])

  if (!checkout.mock) return null

  const payMock = async () => {
    setBusy(true)
    try {
      await new Promise((r) => setTimeout(r, 700))
      await api.post(`/payments/${checkout.paymentId}/mock-pay`)
      onPaid()
    } catch (e) { toast(errorMessage(e), 'err'); setBusy(false) }
  }

  return (
    <Modal title="Complete payment" onClose={busy ? () => {} : onClose}>
      <div className="mb-4 rounded-xl bg-ink p-4 text-white">
        <p className="text-xs uppercase tracking-wide text-ink-300">{checkout.description}</p>
        <p className="mt-1 font-display text-3xl font-extrabold">{inr(checkout.amountPaise / 100)}</p>
        <p className="mt-1 text-xs text-ink-300">Order {checkout.orderId}</p>
      </div>
      <div className="mb-4 grid grid-cols-3 gap-2">
        {METHODS.map(({ id, label, Icon }) => (
          <button key={id} onClick={() => setMethod(id)}
            className={`flex flex-col items-center gap-1 rounded-xl border p-3 text-xs font-semibold transition ${method === id ? 'border-baton bg-baton-50 text-baton-600' : 'border-ink/15 hover:bg-paper-200'}`}>
            <Icon size={18} />{label}
          </button>
        ))}
      </div>
      <button className="btn-primary w-full" disabled={busy} onClick={payMock}>
        {busy ? 'Processing…' : `Pay ${inr(checkout.amountPaise / 100)}`}
      </button>
      <p className="mt-3 flex items-center justify-center gap-1.5 text-center text-xs text-ink-500">
        <ShieldCheck size={14} /> Test mode: no real money moves. Add Razorpay keys on the server to go live.
      </p>
    </Modal>
  )
}
