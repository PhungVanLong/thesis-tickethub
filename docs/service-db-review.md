# Service DB Review — TicketHub

Date: 2026-05-20
Author: (auto)

Purpose: Provide a concise, reviewable summary of the four service databases (identity, management, booking, promotions), highlight responsibilities, main tables/columns, cross-service references, outbox events, sizing & performance notes, security concerns, and a review checklist for a technical review.

---

## Summary
- Four schemas/services: `identity`, `management`, `booking`, `promotions`.
- Goal: isolate business domains, minimize cross-service coupling, keep each service independently deployable and operable.
- Communication pattern: Outbox (per-service) -> message broker (Kafka/RMQ) -> consumers.

---

## 1) Identity Service (schema: `identity`)
Responsibility
- Authentication, user profiles, sessions, platform config, logging/audit for auth-relevant actions.

Key tables
- `identity.users` (id, email, password_hash, roles[], is_active, is_verified, created_at)
- `identity.user_sessions` (id UUID, user_id, refresh_token, expires_at)
- `identity.system_logs` (id, user_id, action, entity_type, entity_id, metadata, created_at)
- `identity.platform_config` (key, value, updated_at)

Cross-service references
- Other services store `user_id` as bigint (no FK). Services must assume `user_id` exists but tolerate missing/soft-deleted entries.

Review focus
- Indexes on `email`, session expiry, token uniqueness.
- Password storage policy and rotation, secrets management.
- Rate-limiting and brute-force logging.

Outbox / events
- Emits: `user.created`, `user.updated`, `user.deleted`, `session.revoked`.

Notes
- Use `identity` as the canonical source-of-truth for user attributes. Keep sensitive fields access-controlled.

---

## 2) Management Service (schema: `management`)
Responsibility
- Event and organizer lifecycle, seat maps, ticket tiers, primary source of event metadata.

Key tables
- `management.organizers` (id, owner_user_id, company_name, status)
- `management.events` (id, organizer_id, title, description, status, start_at, end_at, location JSONB)
- `management.seat_maps` (id, event_id, layout JSONB, total_seats)
- `management.ticket_tiers` (id, event_id, name, price, quantity_total, quantity_sold)
- `management.seats` (id, seat_map_id, seat_code, seat_status, ticket_tier_id)
- `management.analytics_events` (snapshots)

Cross-service references
- Exposes `event_id`, `seat_id`, `ticket_tier_id` to Booking and Promotions (no FKs).

Review focus
- Ensure `seats` and `ticket_tiers` have efficient indexes for read (seat_map_id, seat_status, ticket_tier_id).
- Validate `layout` JSON shape and size (avoid excessively large documents for very large venue maps). Consider row/column denormalization for hot queries.
- Event status transitions and validation rules.

Outbox / events
- Emits: `event.created`, `event.updated`, `seat.updated`, `tier.updated`.

Notes
- Booking service subscribes to `seat.updated`/`event.updated` to denormalize snapshots used during checkout.

---

## 3) Booking Service (schema: `booking`)
Responsibility
- Reservation lifecycle, orders, payments, tickets issuance, outbox for downstream notifications.

Key tables
- `booking.reservations` (id, customer_id, event_id, seat_id, idempotency_key, status, expires_at)
- `booking.orders` (id, customer_id, idempotency_key, order_code, subtotal, promotion_discount, voucher_discount, total_amount, status)
- `booking.order_items` (id, order_id, reservation_id, seat_id, original_price, final_price)
- `booking.payments` (id, order_id, idempotency_key, amount, gateway_name, status)
- `booking.tickets` (id, order_item_id, ticket_code, qr_payload, status)
- `booking.outbox_events` (id UUID, aggregate_type, event_type, payload, status)

Cross-service references
- Stores `event_id` and `seat_id` (management) and `user_id` (identity) as identifiers only. When needed, reads snapshot info previously denormalized.

Review focus
- Reservation TTL enforcement and sweep job correctness.
- Atomic finalize flow (`first-payment-wins`) implemented using database constraints / conditional updates inside booking DB.
- Idempotency uniqueness (idempotency_key unique where required).
- Outbox durability and delivery (retry, dead-letter handling).

Outbox / events
- Emits: `order.created`, `payment.succeeded`, `ticket.issued`, `order.cancelled`.

Notes
- Booking DB must be the source of truth for seat finalization; management receives seat-sold events to update UI caches.

---

## 4) Promotions Service (schema: `promotions`)
Responsibility
- Create/manage vouchers and promotions, enforce usage limits, reservation for voucher claims.

Key tables
- `promotions.vouchers` (id, code, voucher_type, organizer_id, discount_type, discount_value, usage_limit, used_count, per_user_limit, combinable, valid_from, valid_until)
- `promotions.voucher_event_scope` (voucher_id, event_id)
- `promotions.vouchers_usage` (voucher_id, order_id, user_id, discount_applied)
- `promotions.promotion_reservations` (reservation rows for short TTL holds)

Cross-service references
- Returns voucher reservation tokens or validation responses to Booking; Booking persists voucher_id and discount fields in order row.

Review focus
- Concurrency-safe claim/update: use `UPDATE ... WHERE used_count < usage_limit RETURNING id` pattern to avoid over-redemption.
- Reservation TTL semantics and cleanup.
- Auditing of voucher usage.

Outbox / events
- Emits: `voucher.reserved`, `voucher.claimed`, `voucher.released`, `promotion.updated`.

Notes
- Prefer keeping business rules (combinability, per-user limits) inside Promotions; Booking should trust validated response/token when creating orders.

---

## Cross-service interaction patterns
- Use Outbox + broker for eventual consistency. Avoid synchronous cross-service FK checks.
- Denormalize small snapshots to reduce cross-service reads at checkout (e.g., store `event_snapshot` and `seat_snapshot` inside `booking.orders` or `booking.order_items`).
- Idempotency: require and validate `idempotency_key` on reservation, order creation, and payment webhook.
- Reconciliation: implement periodic sweep jobs to compare counts and detect drift; expose reconciliation endpoints.

---

## Review checklist (handy for reviewers)
- Schema correctness
  - [ ] Tables are scoped to one service only (no cross-schema FKs).
  - [ ] Key columns and unique constraints exist for idempotency and dedupe.
  - [ ] Indexes exist for hot read patterns (seat lookups, order lookups, payment status).
- Concurrency & correctness
  - [ ] Promotions claim uses atomic DB update pattern.
  - [ ] Booking finalize / seat sell uses conditional update or unique constraint to enforce first-payment-wins.
  - [ ] Reservation TTL implemented and sweep job documented.
- Availability & scaling
  - [ ] Outbox tables have indexes and retention strategy.
  - [ ] Large JSON fields (seat layout) are validated and not used for hot joins.
- Security & compliance
  - [ ] Sensitive columns (password_hash) access is restricted and encrypted at rest.
  - [ ] Audit logging exists for voucher claims and payment events.
- Migration & operations
  - [ ] Backfill and ETL plan exists to populate schemas from monolith.
  - [ ] Reconciliation queries available to detect drift.
- Testing
  - [ ] Load tests cover `POST /reservations`, `POST /orders`, voucher claims under contention.
  - [ ] Chaos tests for broker and Outbox delivery failure scenarios.

---

## Suggested next actions (minimal deliverables for review meeting)
1. Export ERD per schema (PNG/SVG) and attach to PR.
2. Provide sample SQL for critical atomic operations:
   - Promotions claim (atomic increment),
   - Booking finalize (conditional seat update / insert ticket)
3. Add reconciliation queries and a short migration playbook (dual-write approach).
4. Run a walkthrough of event flows with sample payloads for Outbox events.

---

## Files created/updated
- Schema DDL: `01_DATABASE_SCHEMA.sql` (contains multi-schema DDL)
- Review doc: this file (`docs/service-db-review.md`)

---

If you want, I can now:
- generate per-schema ER diagrams (SVG), or
- add the sample SQL snippets mentioned in step 2, or
- produce the migration playbook (dual-write + ETL) as the next doc.

Select: `ERD`, `SQL`, or `MIGRATE` to continue.
