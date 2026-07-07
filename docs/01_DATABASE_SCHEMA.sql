-- ============================================
-- TICKETHUB - DATABASE SCHEMA (PostgreSQL)
-- Optimized into 4 service schemas: identity, management, booking, promotions
-- Generated: 2026-05-20
-- ============================================

-- NOTE: For service isolation we create separate schemas. Cross-service references
-- are represented by identifier columns (no cross-schema foreign keys) to avoid
-- tight coupling. Use Outbox events to propagate changes between services.

-- Create schemas
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS management;
CREATE SCHEMA IF NOT EXISTS booking;
CREATE SCHEMA IF NOT EXISTS promotions;

-- ============================================
-- SCHEMA: identity (Auth, users, sessions, audit)
-- ============================================
CREATE TABLE identity.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    phone VARCHAR(32),
    first_name VARCHAR(128),
    last_name VARCHAR(128),
    roles TEXT[] DEFAULT ARRAY['CUSTOMER'],
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON identity.users(email);

CREATE TABLE identity.user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    refresh_token TEXT UNIQUE,
    device_fingerprint TEXT,
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON identity.user_sessions(user_id);

CREATE TABLE identity.system_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id BIGINT,
    ip_address INET,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON identity.system_logs(user_id);

CREATE TABLE identity.platform_config (
    key VARCHAR(200) PRIMARY KEY,
    value TEXT,
    description TEXT,
    updated_by BIGINT,
    version INT DEFAULT 1,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- SCHEMA: management (events, organizers, seat maps)
-- ============================================
CREATE TABLE management.organizers (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL, -- ref to identity.users.id (no FK)
    company_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    commission_rate NUMERIC(5,2) DEFAULT 5.00,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE management.events (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL, -- management.organizers.id
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    location JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON management.events(organizer_id);
CREATE INDEX ON management.events(status);

CREATE TABLE management.seat_maps (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL, -- management.events.id
    name TEXT,
    layout JSONB,
    total_seats INT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE management.ticket_tiers (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    quantity_total INT NOT NULL,
    quantity_sold INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE management.seats (
    id BIGSERIAL PRIMARY KEY,
    seat_map_id BIGINT NOT NULL,
    seat_code VARCHAR(64) NOT NULL,
    row_label VARCHAR(16),
    col_number INT,
    seat_status VARCHAR(30) DEFAULT 'AVAILABLE', -- AVAILABLE/RESERVED/SOLD
    ticket_tier_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(seat_map_id, seat_code)
);
CREATE INDEX ON management.seats(seat_map_id);
CREATE INDEX ON management.seats(seat_status);

CREATE TABLE management.analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT,
    total_tickets_sold INT DEFAULT 0,
    total_checkins INT DEFAULT 0,
    total_revenue NUMERIC(18,2) DEFAULT 0,
    snapshot_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- SCHEMA: promotions (vouchers, promotions, usage)
-- ============================================
CREATE TABLE promotions.vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    voucher_type VARCHAR(30) NOT NULL, -- ORGANIZER | SYSTEM
    organizer_id BIGINT, -- nullable, scope to organizer (management.organizers.id)
    apply_on VARCHAR(30) DEFAULT 'ALL', -- ALL | SPECIFIC_EVENTS
    discount_type VARCHAR(20) NOT NULL, -- PERCENT | FIXED
    discount_value NUMERIC(12,4) NOT NULL,
    min_order_value NUMERIC(12,2) DEFAULT 0,
    usage_limit INT DEFAULT NULL,
    used_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,
    combinable BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    valid_from TIMESTAMPTZ,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE promotions.voucher_event_scope (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL, -- promotions.vouchers.id
    event_id BIGINT NOT NULL -- management.events.id
);

CREATE TABLE promotions.vouchers_usage (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL, -- booking.orders.id
    user_id BIGINT NOT NULL, -- identity.users.id
    discount_applied NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE promotions.promotion_reservations (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reserved_until TIMESTAMPTZ NOT NULL,
    order_reference TEXT, -- temporary token to link reservation
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE promotions.ticket_promotions (
    id BIGSERIAL PRIMARY KEY,
    ticket_tier_id BIGINT NOT NULL, -- management.ticket_tiers.id
    name VARCHAR(200),
    promo_type VARCHAR(30), -- PERCENT | FIXED | NEW_PRICE
    discount_value NUMERIC(12,4),
    quantity_limit INT,
    quantity_sold INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- SCHEMA: booking (reservations, orders, payments, tickets, outbox)
-- ============================================
CREATE TABLE booking.reservations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL, -- identity.users.id
    event_id BIGINT NOT NULL, -- management.events.id
    seat_id BIGINT NOT NULL, -- management.seats.id
    ticket_tier_id BIGINT,
    idempotency_key VARCHAR(200),
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING_PAYMENT, ORDERED, CANCELLED, EXPIRED
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON booking.reservations(customer_id);
CREATE INDEX ON booking.reservations(event_id);
CREATE INDEX ON booking.reservations(status);

CREATE TABLE booking.orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    idempotency_key VARCHAR(200) UNIQUE,
    order_code VARCHAR(100) UNIQUE,
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING | PAID | FAILED | REFUNDED
    subtotal NUMERIC(12,2) NOT NULL,
    promotion_discount NUMERIC(12,2) DEFAULT 0,
    voucher_discount NUMERIC(12,2) DEFAULT 0,
    total_amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(8) DEFAULT 'VND',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON booking.orders(customer_id);
CREATE INDEX ON booking.orders(status);

CREATE TABLE booking.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    reservation_id BIGINT, -- booking.reservations.id
    seat_id BIGINT,
    ticket_tier_id BIGINT,
    original_price NUMERIC(12,2),
    final_price NUMERIC(12,2),
    promotion_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE booking.payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    idempotency_key VARCHAR(200) UNIQUE,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(8) DEFAULT 'VND',
    gateway_name VARCHAR(50),
    gateway_tx_id VARCHAR(255),
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING|SUCCESS|FAILED|REFUNDED
    processed_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON booking.payments(order_id);
CREATE INDEX ON booking.payments(status);

CREATE TABLE booking.tickets (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    seat_id BIGINT,
    ticket_code VARCHAR(100) UNIQUE,
    qr_payload JSONB NOT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ
);
CREATE INDEX ON booking.tickets(ticket_code);

-- Outbox for reliable events (per booking service)
CREATE TABLE booking.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100),
    aggregate_id BIGINT,
    event_type VARCHAR(200),
    payload JSONB,
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING|PUBLISHED|FAILED
    retry_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    published_at TIMESTAMPTZ
);
CREATE INDEX ON booking.outbox_events(status);

CREATE TABLE booking.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    channel VARCHAR(30), -- email|websocket|push
    type VARCHAR(100),
    payload JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- UTILITY: helper functions and comments
-- ============================================
-- Extension required for gen_random_uuid()
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Concurrency-safe voucher claim example (run in promotions DB):
-- UPDATE promotions.vouchers
-- SET used_count = used_count + 1
-- WHERE id = $1 AND (usage_limit IS NULL OR used_count < usage_limit)
-- RETURNING id;

-- ============================================
-- END OF SCHEMA
-- ============================================
CREATE INDEX idx_email_notifications_created_at ON email_notifications(created_at);

CREATE TABLE web_push_subscriptions (
    subscription_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    endpoint VARCHAR(2048) UNIQUE NOT NULL,
    p256dh VARCHAR(255),
    auth VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_web_push_subscriptions_user_id ON web_push_subscriptions(user_id);

-- ============================================
-- MODULE 10: SECURITY & BOT DETECTION
-- ============================================

CREATE TABLE rate_limit_attempts (
    attempt_id BIGSERIAL PRIMARY KEY,
    ip_address INET,
    user_id BIGINT REFERENCES users(user_id),
    request_date DATE,
    request_hour INT,
    hour_requests INT DEFAULT 0,
    day_requests INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ip_address, user_id, request_date, request_hour)
);

CREATE INDEX idx_rate_limit_attempts_ip ON rate_limit_attempts(ip_address);
CREATE INDEX idx_rate_limit_attempts_user ON rate_limit_attempts(user_id);
CREATE INDEX idx_rate_limit_attempts_date_hour ON rate_limit_attempts(request_date, request_hour);

CREATE TABLE bot_detections (
    detection_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    ip_address INET,
    device_fingerprint VARCHAR(255),
    detection_type ENUM('RATE_LIMIT', 'HONEYPOT', 'CAPTCHA', 'BEHAVIOR', 'GEO_ANOMALY') NOT NULL,
    risk_score INT CHECK (risk_score >= 0 AND risk_score <= 100),
    description TEXT,
    action_taken ENUM('BLOCK', 'CHALLENGE', 'LOG', 'NONE') DEFAULT 'LOG',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bot_detections_user_id ON bot_detections(user_id);
CREATE INDEX idx_bot_detections_ip_address ON bot_detections(ip_address);
CREATE INDEX idx_bot_detections_detection_type ON bot_detections(detection_type);
CREATE INDEX idx_bot_detections_created_at ON bot_detections(created_at);

CREATE TABLE api_keys (
    api_key_id BIGSERIAL PRIMARY KEY,
    api_key_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    key_type ENUM('LOAD_TEST', 'WEBHOOK', 'INTEGRATION') DEFAULT 'INTEGRATION',
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at);

-- ============================================
-- MODULE 11: ANALYTICS & REPORTING
-- ============================================

CREATE TABLE event_analytics (
    analytics_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    analytics_date DATE NOT NULL,
    views INT DEFAULT 0,
    clicks INT DEFAULT 0,
    bookings INT DEFAULT 0,
    revenue DECIMAL(15, 2) DEFAULT 0,
    seats_sold INT DEFAULT 0,
    seats_percentage DECIMAL(5, 2) DEFAULT 0,
    booking_rate DECIMAL(5, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id, analytics_date)
);

CREATE INDEX idx_event_analytics_event_id ON event_analytics(event_id);
CREATE INDEX idx_event_analytics_date ON event_analytics(analytics_date);

CREATE TABLE system_analytics (
    analytics_id BIGSERIAL PRIMARY KEY,
    analytics_date DATE UNIQUE NOT NULL,
    total_users INT DEFAULT 0,
    active_users INT DEFAULT 0,
    new_users INT DEFAULT 0,
    total_bookings INT DEFAULT 0,
    total_revenue DECIMAL(15, 2) DEFAULT 0,
    total_refunds DECIMAL(15, 2) DEFAULT 0,
    total_commission DECIMAL(15, 2) DEFAULT 0,
    avg_booking_value DECIMAL(10, 2) DEFAULT 0,
    payment_success_rate DECIMAL(5, 2) DEFAULT 0,
    bot_detections INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_analytics_date ON system_analytics(analytics_date);

CREATE TABLE user_activity_log (
    activity_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    activity_type VARCHAR(100),
    event_id BIGINT REFERENCES events(event_id),
    metadata JSONB,
    ip_address INET,
    device_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_activity_log_user_id ON user_activity_log(user_id);
CREATE INDEX idx_user_activity_log_activity_type ON user_activity_log(activity_type);
CREATE INDEX idx_user_activity_log_created_at ON user_activity_log(created_at);

-- ============================================
-- MODULE 12: WISHLIST & REVIEWS (Optional)
-- ============================================

CREATE TABLE wishlists (
    wishlist_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);

CREATE INDEX idx_wishlists_user_id ON wishlists(user_id);
CREATE INDEX idx_wishlists_event_id ON wishlists(event_id);

CREATE TABLE event_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating INT CHECK (rating >= 1 AND rating <= 5) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_reviews_event_id ON event_reviews(event_id);
CREATE INDEX idx_event_reviews_user_id ON event_reviews(user_id);

-- ============================================
-- OFFLINE CHECKIN CACHE (for mobile devices)
-- ============================================

CREATE TABLE offline_checkin_queue (
    queue_id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50),
    device_id VARCHAR(255),
    sync_status ENUM('PENDING', 'SYNCED', 'CONFLICT', 'INVALID') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP,
    conflict_note TEXT
);

CREATE INDEX idx_offline_checkin_queue_sync_status ON offline_checkin_queue(sync_status);
CREATE INDEX idx_offline_checkin_queue_device_id ON offline_checkin_queue(device_id);

-- ============================================
-- END OF SCHEMA
-- ============================================

