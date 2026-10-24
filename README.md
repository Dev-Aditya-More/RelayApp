# Relay

Work orders, SLA tracking and payment orchestration. Citizens report issues, admins hand them to contractors, contractors deliver, and Relay tracks the clock and settles the money.

**Stack:** React + Vite + Tailwind + Axios · Spring Boot 3 + Spring Security (JWT) + JPA · PostgreSQL (H2 for zero-setup dev) · Razorpay · Docker + GitHub Actions

## Run it locally (no Docker, no Postgres)

```bash
# terminal 1: API on :8080 (needs Java 25; uses an embedded H2 file DB and seeds demo data)
cd backend && ./mvnw spring-boot:run

# terminal 2: UI on :5173 (proxies /api to :8080)
cd frontend && npm install && npm run dev
```

Local defaults (dev profile only): admin `admin@relay.app` / `admin123`. Seeded demo contractors and citizens share the password `demo12345`.
The login page does not advertise them; admins see them under **People**.

## Accounts and admins

- **Citizens** sign up freely. **Contractors** sign up too but stay *awaiting approval* until an admin approves them in **People**; unapproved contractors cannot be assigned work.
- **Admins are never self-service.** The first admin is created at startup from `ADMIN_EMAIL` + `ADMIN_PASSWORD` (8+ chars; ignored if that email already exists). Further admins: a current admin clicks **Make admin** on the person in **People**. Admins can also suspend or restore any account.
- **Demo data:** set `SEED_DEMO_DATA=true` and `DEMO_PASSWORD` to seed demo contractors, citizens and sample orders, flagged as demo and listed to admins only. Use `SEED_DEMO_DATA=false` for a clean production database.

## Password reset and email

"Forgot password?" emails a one-time link (valid 30 minutes, stored only as a hash, one request per minute per account). Using it signs the user out everywhere else.
Email goes through [Resend](https://resend.com): set `RESEND_API_KEY`, `MAIL_FROM` (a verified sender, e.g. `Relay <no-reply@yourdomain.com>`) and `APP_URL` (your Vercel URL, no trailing slash).
With no key set, the email is printed in the server log instead, which is how local dev works.

## Email verification and rate limiting

- **Verification:** sign-up emails a 24-hour link. Until it is used, an account can sign in but cannot report issues, be assigned work, or be approved as a contractor. Resetting a password also verifies the email. `REQUIRE_EMAIL_VERIFICATION=false` skips this (default off in dev, on in production).
- **Rate limits (per IP):** sign-in 20/min, sign-up 10/hour, forgot-password 5 per 15 min, reset and verify endpoints similar, everything else 600/min. Razorpay's webhook and `/api/health` are exempt. 429 responses carry `Retry-After`.
- **Sign-in lockout:** 5 wrong passwords for one email locks that email for 15 minutes from any IP. Resetting the password still works.
- Counters live in memory, so they reset on restart and are per instance. Move them to Redis before running more than one backend instance. Disable with `RATE_LIMIT_ENABLED=false`.

## Run with Docker + PostgreSQL

```bash
docker compose up --build   # UI at http://localhost:8081
```

## Razorpay

With no keys set, payments run in **mock mode**: the same order, ledger and invoice flow, with a stand-in checkout sheet.
To go live (test or production), set `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET` and `RAZORPAY_WEBHOOK_SECRET`, and point a Razorpay webhook (`payment.captured`, `payment.failed`) at `POST /api/payments/webhook`.
Checkout signatures and webhook signatures are verified with HMAC-SHA256; the webhook is idempotent.

## What is in the MVP

- **Roles:** Citizen, Contractor, Admin (JWT, method-level authorization).
- **Work orders:** report with photo + GPS, auto-generated code (`RLY-000042`), assign, start, complete with photo proof, citizen rating closes it, full timeline/audit trail.
- **Relay Assist:** keyword classifier suggests category + severity as the citizen types; sentiment tagging on feedback.
- **SLA:** per-category policies (editable by admin), live countdown, a scheduler that flags breaches every minute and escalates unassigned ones.
- **Delay prediction:** transparent 0-99 risk score from SLA elapsed time, assignment state, contractor load and category history.
- **Payments:** fast-track fee (halves the SLA), SLA penalty on contractors, contractor payout, history and printable invoices.
- **Admin analytics:** KPIs, hotspot map, 7-day trend, at-risk orders, contractor scoreboard, money moved.
- **In-app notifications** (email/SMS delivery is simulated in the server log).

## Known gaps

- Contractor payouts are recorded as simulated; real settlement needs RazorpayX.
- Relay Assist is rule-based, not an image model. Multi-tenancy, subscriptions, multi-language UI and PDF/Excel export are not built.
- No automated tests yet; CI builds both apps.
- Set `JWT_SECRET`, `ADMIN_PASSWORD` and (if you seed demo data) `DEMO_PASSWORD` before any real deployment.
