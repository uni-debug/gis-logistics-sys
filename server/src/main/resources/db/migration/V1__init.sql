-- V1__init.sql
-- GIS logistics core schema (MySQL 8, SRID 4326)
-- Flyway runs this against the configured schema.

CREATE TABLE users (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phone_enc     VARBINARY(256) NOT NULL,
    phone_hash    CHAR(64) NOT NULL,
    name          VARCHAR(64) NOT NULL,
    avatar        VARCHAR(512) NULL,
    status        TINYINT NOT NULL DEFAULT 1,
    role          VARCHAR(16) NOT NULL DEFAULT 'user',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_phone_hash UNIQUE (phone_hash)
);
CREATE INDEX idx_users_status ON users (status);

CREATE TABLE sites (
    id        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code      VARCHAR(32) NOT NULL,
    name      VARCHAR(128) NOT NULL,
    point     GEOMETRY NOT NULL,
    capacity  INT NOT NULL DEFAULT 0,
    status    TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_sites_code UNIQUE (code),
    SPATIAL INDEX idx_sites_point (point)
);

CREATE TABLE staff_profiles (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    site_id       BIGINT NOT NULL,
    license_no    VARCHAR(64) NOT NULL,
    delivery_area GEOMETRY NOT NULL,
    status        TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_staff_user UNIQUE (user_id),
    CONSTRAINT uk_staff_license UNIQUE (license_no),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_staff_site FOREIGN KEY (site_id) REFERENCES sites (id),
    SPATIAL INDEX idx_staff_delivery_area (delivery_area)
);

CREATE TABLE admin_profiles (
    id        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT NOT NULL,
    perm_mask BIGINT NOT NULL DEFAULT 0,
    is_super  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_admin_user UNIQUE (user_id),
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE demands (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    title         VARCHAR(128) NOT NULL,
    weight_g      INT NOT NULL DEFAULT 0,
    volume_cm3    INT NOT NULL DEFAULT 0,
    fragile       BOOLEAN NOT NULL DEFAULT FALSE,
    origin_region VARCHAR(64) NOT NULL,
    origin_addr   VARCHAR(256) NOT NULL,
    target_region VARCHAR(64) NOT NULL,
    target_addr   VARCHAR(256) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    quoted_price  INT NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_demand_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_demand_user_status (user_id, status),
    KEY idx_demand_regions (origin_region, target_region)
);

CREATE TABLE orders (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    demand_id     BIGINT NOT NULL,
    staff_id      BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    amount        INT NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    version       INT NOT NULL DEFAULT 0,
    paid_at       DATETIME NULL,
    delivered_at  DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_demand FOREIGN KEY (demand_id) REFERENCES demands (id),
    CONSTRAINT fk_order_staff FOREIGN KEY (staff_id) REFERENCES staff_profiles (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_order_user_status (user_id, status),
    KEY idx_order_staff_status (staff_id, status),
    KEY idx_order_demand (demand_id)
);

CREATE TABLE payments (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    channel    VARCHAR(16) NOT NULL,
    txn_no     VARCHAR(64) NOT NULL,
    amount     INT NOT NULL DEFAULT 0,
    status     VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uk_pay_order_txn UNIQUE (order_id, txn_no),
    KEY idx_pay_order_status (order_id, status)
);

CREATE TABLE payment_logs (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id   BIGINT NOT NULL,
    from_status  VARCHAR(16) NOT NULL,
    to_status    VARCHAR(16) NOT NULL,
    payload      JSON NULL,
    occurred_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paylog_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE route_tasks (
    id             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT NOT NULL,
    strategy       VARCHAR(16) NOT NULL,
    from_point     GEOMETRY NOT NULL,
    to_point       GEOMETRY NOT NULL,
    via_points     GEOMETRY NOT NULL,
    avoid_segments VARCHAR(512) NULL,
    geometry       GEOMETRY NOT NULL,
    distance_m     DOUBLE NULL,
    eta_s          INT NULL,
    cost           INT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_order FOREIGN KEY (order_id) REFERENCES orders (id),
    KEY idx_route_order_created (order_id, created_at),
    SPATIAL INDEX idx_route_geometry (geometry)
);

CREATE TABLE logistics_events (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    type        VARCHAR(16) NOT NULL,
    point       GEOMETRY NOT NULL,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id BIGINT NULL,
    CONSTRAINT fk_event_order FOREIGN KEY (order_id) REFERENCES orders (id),
    KEY idx_event_order_time (order_id, occurred_at),
    SPATIAL INDEX idx_event_point (point)
);

CREATE TABLE warehouse_records (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    site_id     BIGINT NOT NULL,
    order_id    BIGINT NOT NULL,
    action      VARCHAR(16) NOT NULL,
    quantity    INT NOT NULL DEFAULT 0,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wh_site FOREIGN KEY (site_id) REFERENCES sites (id),
    CONSTRAINT fk_wh_order FOREIGN KEY (order_id) REFERENCES orders (id),
    KEY idx_wh_site_time (site_id, occurred_at),
    KEY idx_wh_order (order_id)
);

CREATE TABLE reviews (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    staff_id     BIGINT NOT NULL,
    rating       TINYINT NOT NULL DEFAULT 5,
    content      TEXT NULL,
    image_urls   JSON NULL,
    staff_reply  TEXT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders (id),
    KEY idx_review_order (order_id),
    KEY idx_review_staff_rating (staff_id, rating)
);

CREATE TABLE notices (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(128) NOT NULL,
    body       TEXT NOT NULL,
    status     TINYINT NOT NULL DEFAULT 1,
    pinned     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notice_status_pinned (status, pinned)
);

CREATE TABLE feedbacks (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       VARCHAR(16) NOT NULL,
    content    TEXT NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    reply      TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_feedback_type_status (type, status)
);

CREATE TABLE im_sessions (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    admin_id      BIGINT NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_msg_at   DATETIME NULL,
    CONSTRAINT fk_imsession_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_imsession_user (user_id)
);

CREATE TABLE im_messages (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id   BIGINT NOT NULL,
    sender_role  VARCHAR(8) NOT NULL,
    content      TEXT NOT NULL,
    sent_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_immsg_session FOREIGN KEY (session_id) REFERENCES im_sessions (id),
    KEY idx_immsg_session_time (session_id, sent_at)
);
