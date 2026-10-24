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

Demo logins (the login page has one-click buttons):

| Role | Email | Password |
|---|---|---|
| Admin | admin@relay.app | admin123 |
| Contractor | ravi@relay.app | contractor123 |
| Citizen | asha@relay.app | citizen123 |

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
- Change `JWT_SECRET` and the seeded passwords before any real deployment.
