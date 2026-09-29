CREATE TABLE staff_applications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    site_id       BIGINT       NOT NULL,
    license_no    VARCHAR(64)  NOT NULL,
    delivery_area GEOMETRY     NOT NULL,
    reason        VARCHAR(512) NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    admin_id      BIGINT       NULL,
    decided_at    DATETIME     NULL,
    created_at    DATETIME     NOT NULL,
    UNIQUE KEY uk_application_user (user_id),
    SPATIAL INDEX idx_staff_app_delivery_area (delivery_area)
);
