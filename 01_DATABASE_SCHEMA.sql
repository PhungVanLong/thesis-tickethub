-- ============================================
-- TICKETHUB - DATABASE SCHEMA (PostgreSQL)
-- Version: 1.0
-- Created: 2026-04-21
-- ============================================

-- ============================================
-- MODULE 1: AUTHENTICATION & USER MANAGEMENT
-- ============================================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role ENUM('GUEST', 'CUSTOMER', 'ORGANIZER', 'STAFF', 'ADMIN') NOT NULL DEFAULT 'GUEST',
    kyc_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    avatar_url VARCHAR(2048),
    is_active BOOLEAN DEFAULT TRUE,
    is_banned BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    permissions JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    refresh_token VARCHAR(500) UNIQUE NOT NULL,
    device_fingerprint VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expires_at CHECK (expires_at > created_at)
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_refresh_token ON user_sessions(refresh_token);

-- ============================================
-- MODULE 2: ORGANIZER & MERCHANT MANAGEMENT
-- ============================================

CREATE TABLE organizers (
    organizer_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(50),
    bank_account VARCHAR(50),
    bank_name VARCHAR(255),
    business_license_url VARCHAR(2048),
    status ENUM('PENDING', 'APPROVED', 'SUSPENDED', 'BANNED') DEFAULT 'PENDING',
    commission_rate DECIMAL(5, 2) DEFAULT 5.00,
    total_revenue DECIMAL(15, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_organizers_status ON organizers(status);
CREATE INDEX idx_organizers_user_id ON organizers(user_id);

CREATE TABLE organizer_payouts (
    payout_id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL REFERENCES organizers(organizer_id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    bank_transfer_ref VARCHAR(100),
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payout_amount CHECK (amount > 0)
);

CREATE INDEX idx_organizer_payouts_organizer_id ON organizer_payouts(organizer_id);
CREATE INDEX idx_organizer_payouts_status ON organizer_payouts(status);

-- ============================================
-- MODULE 3: EVENT MANAGEMENT
-- ============================================

CREATE TABLE event_locations (
    location_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, city)
);

CREATE INDEX idx_event_locations_city ON event_locations(city);
CREATE INDEX idx_event_locations_name ON event_locations(name);

CREATE TABLE events (
    event_id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL REFERENCES organizers(organizer_id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description LONGTEXT,
    poster_url VARCHAR(2048),
    category VARCHAR(100),
    status ENUM('DRAFT', 'PENDING', 'APPROVED', 'ACTIVE', 'CANCELLED', 'COMPLETED') DEFAULT 'DRAFT',
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    location_id BIGINT REFERENCES event_locations(location_id),
    total_capacity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_event_dates CHECK (end_at > start_at)
);

CREATE INDEX idx_events_organizer_id ON events(organizer_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_start_at ON events(start_at);
CREATE INDEX idx_events_title ON events(title);

CREATE TABLE event_ticket_types (
    ticket_type_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    quantity_total INT NOT NULL,
    quantity_sold INT DEFAULT 0,
    quantity_locked INT DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    start_sale_at TIMESTAMP,
    end_sale_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quantities CHECK (quantity_sold + quantity_locked <= quantity_total),
    CONSTRAINT chk_price CHECK (price >= 0)
);

CREATE INDEX idx_event_ticket_types_event_id ON event_ticket_types(event_id);
CREATE INDEX idx_event_ticket_types_status ON event_ticket_types(status);

CREATE TABLE event_discounts (
    discount_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent DECIMAL(5, 2),
    discount_amount DECIMAL(10, 2),
    usage_limit INT,
    used_count INT DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_usage CHECK (used_count <= usage_limit OR usage_limit IS NULL)
);

CREATE INDEX idx_event_discounts_event_id ON event_discounts(event_id);
CREATE INDEX idx_event_discounts_code ON event_discounts(code);

-- ============================================
-- MODULE 4: SEAT & VENUE MANAGEMENT
-- ============================================

CREATE TABLE seat_maps (
    seat_map_id BIGSERIAL PRIMARY KEY,
    event_id BIGINT UNIQUE NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    total_seats INT NOT NULL,
    rows INT NOT NULL,
    cols INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_seat_map_dimensions CHECK (rows > 0 AND cols > 0 AND total_seats = rows * cols)
);

CREATE TABLE seats (
    seat_id BIGSERIAL PRIMARY KEY,
    seat_map_id BIGINT NOT NULL REFERENCES seat_maps(seat_map_id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    row_label VARCHAR(5),
    col_number INT,
    seat_type ENUM('NORMAL', 'VIP', 'BLOCKED', 'EXIT') DEFAULT 'NORMAL',
    status ENUM('AVAILABLE', 'LOCKED', 'BOOKED', 'RESERVED') DEFAULT 'AVAILABLE',
    ticket_type_id BIGINT REFERENCES event_ticket_types(ticket_type_id),
    locked_until TIMESTAMP,
    locked_by_booking_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(seat_map_id, seat_number)
);

CREATE INDEX idx_seats_seat_map_id ON seats(seat_map_id);
CREATE INDEX idx_seats_status ON seats(status);
CREATE INDEX idx_seats_locked_until ON seats(locked_until);
CREATE INDEX idx_seats_ticket_type_id ON seats(ticket_type_id);

-- ============================================
-- MODULE 5: BOOKING & RESERVATION
-- ============================================

CREATE TABLE bookings (
    booking_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    status ENUM('PENDING', 'LOCKED', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    total_price DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    commission_amount DECIMAL(10, 2) DEFAULT 0,
    booking_code VARCHAR(50) UNIQUE NOT NULL,
    idempotency_key VARCHAR(100),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT chk_booking_price CHECK (total_price >= 0),
    CONSTRAINT chk_booking_dates CHECK (completed_at IS NULL OR completed_at >= created_at)
);

CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX idx_bookings_event_id ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_booking_code ON bookings(booking_code);
CREATE INDEX idx_bookings_idempotency_key ON bookings(idempotency_key);
CREATE INDEX idx_bookings_expires_at ON bookings(expires_at);

CREATE TABLE booking_seats (
    booking_seat_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(seat_id) ON DELETE CASCADE,
    price DECIMAL(10, 2) NOT NULL,
    UNIQUE(booking_id, seat_id)
);

CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
CREATE INDEX idx_booking_seats_seat_id ON booking_seats(seat_id);

-- ============================================
-- MODULE 6: PAYMENT & TRANSACTION
-- ============================================

CREATE TABLE payments (
    payment_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES users(user_id),
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method ENUM('VNPAY', 'MOMO', 'BANK_TRANSFER', 'PAYPAL') NOT NULL,
    gateway_transaction_id VARCHAR(255),
    status ENUM('PENDING', 'INITIATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    error_code VARCHAR(50),
    error_message TEXT,
    paid_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_transaction_id ON payments(gateway_transaction_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);

CREATE TABLE refunds (
    refund_id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(payment_id) ON DELETE CASCADE,
    reason ENUM('CUSTOMER_REQUEST', 'PAYMENT_FAIL', 'EVENT_CANCELLED', 'DUPLICATE') DEFAULT 'CUSTOMER_REQUEST',
    amount DECIMAL(15, 2) NOT NULL,
    refund_policy_percent INT CHECK (refund_policy_percent IN (0, 80, 100)),
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    gateway_refund_id VARCHAR(255),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_refund_amount CHECK (amount > 0)
);

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_requested_at ON refunds(requested_at);

-- ============================================
-- MODULE 7: TICKET & QR CODE
-- ============================================

CREATE TABLE tickets (
    ticket_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(seat_id),
    ticket_code VARCHAR(50) UNIQUE NOT NULL,
    qr_code_data TEXT NOT NULL,
    qr_image_url VARCHAR(2048),
    status ENUM('ACTIVE', 'CHECKED_IN', 'CANCELLED', 'EXPIRED') DEFAULT 'ACTIVE',
    check_in_at TIMESTAMP,
    check_in_by_staff_id BIGINT REFERENCES users(user_id),
    checked_in_device_id VARCHAR(255),
    event_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(booking_id, seat_id)
);

CREATE INDEX idx_tickets_ticket_code ON tickets(ticket_code);
CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_check_in_at ON tickets(check_in_at);
CREATE INDEX idx_tickets_event_date ON tickets(event_date);

CREATE TABLE qr_code_generations (
    qr_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    qr_data TEXT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_qr_code_generations_ticket_id ON qr_code_generations(ticket_id);

-- ============================================
-- MODULE 8: CHECK-IN & STAFF MANAGEMENT
-- ============================================

CREATE TABLE staff_assignments (
    assignment_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    organizer_id BIGINT NOT NULL REFERENCES organizers(organizer_id) ON DELETE CASCADE,
    role ENUM('STAFF', 'CHECK_IN_MANAGER', 'SUPPORT_STAFF') DEFAULT 'STAFF',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, organizer_id)
);

CREATE INDEX idx_staff_assignments_organizer_id ON staff_assignments(organizer_id);
CREATE INDEX idx_staff_assignments_user_id ON staff_assignments(user_id);

CREATE TABLE check_ins (
    checkin_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES users(user_id),
    event_id BIGINT NOT NULL REFERENCES events(event_id),
    device_id VARCHAR(255),
    location_latitude DECIMAL(10, 8),
    location_longitude DECIMAL(11, 8),
    checked_in_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE INDEX idx_check_ins_ticket_id ON check_ins(ticket_id);
CREATE INDEX idx_check_ins_staff_id ON check_ins(staff_id);
CREATE INDEX idx_check_ins_event_id ON check_ins(event_id);
CREATE INDEX idx_check_ins_checked_in_at ON check_ins(checked_in_at);

CREATE TABLE audit_logs (
    audit_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    old_value JSONB,
    new_value JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ============================================
-- MODULE 9: NOTIFICATION & COMMUNICATION
-- ============================================

CREATE TABLE email_notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body LONGTEXT,
    template_name VARCHAR(100),
    related_booking_id BIGINT REFERENCES bookings(booking_id),
    status ENUM('PENDING', 'SENT', 'FAILED', 'BOUNCED') DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_notifications_user_id ON email_notifications(user_id);
CREATE INDEX idx_email_notifications_status ON email_notifications(status);
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

