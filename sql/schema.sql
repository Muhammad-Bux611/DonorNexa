-- =============================================================================
-- DonorNexa — Relational Database Schema (3NF)
-- DBMS: MySQL 8.0 / MariaDB Compatible
-- Character Set: UTF-8 (utf8mb4 / utf8mb4_unicode_ci)
-- =============================================================================

DROP DATABASE IF EXISTS donornexa_db;
CREATE DATABASE donornexa_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE donornexa_db;

-- -----------------------------------------------------------------------------
-- 1. Table: users
-- Core authentication entity for all portal actors (Admin, Hospital, Donor).
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(25) NOT NULL,
    role ENUM('ADMIN', 'HOSPITAL', 'DONOR') NOT NULL,
    account_status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION') NOT NULL DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    lockout_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    -- Index justification: Email is queried on every single login attempt
    INDEX idx_users_email (email),
    -- Index justification: Role filtering is used by administrative user management
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 2. Table: provinces
-- Administrative provinces/territories of Pakistan for geographical partitioning.
-- -----------------------------------------------------------------------------
CREATE TABLE provinces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    CONSTRAINT uk_provinces_code UNIQUE (code),
    INDEX idx_provinces_code (code)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 3. Table: cities
-- Cities in Pakistan linked to provinces to support distance and proximity matching.
-- -----------------------------------------------------------------------------
CREATE TABLE cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    province_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    CONSTRAINT fk_cities_province FOREIGN KEY (province_id)
        REFERENCES provinces (id) ON DELETE RESTRICT,
    -- Index justification: Proximity queries search cities grouped by province
    INDEX idx_cities_province (province_id),
    -- Index justification: Fast lookup during user registration cascading dropdowns
    INDEX idx_cities_name (name)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 4. Table: donors
-- Voluntary blood donor profiles linked 1-to-1 with a user record.
-- -----------------------------------------------------------------------------
CREATE TABLE donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    blood_group ENUM('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE') NOT NULL,
    date_of_birth DATE NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
    weight DECIMAL(5, 2) NULL,
    province_id BIGINT NOT NULL,
    city_id BIGINT NOT NULL,
    complete_address VARCHAR(500) NULL,
    last_donation_date DATE NULL,
    availability ENUM('AVAILABLE', 'TEMPORARILY_UNAVAILABLE', 'UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
    photo VARCHAR(255) NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_donors_user UNIQUE (user_id),
    CONSTRAINT fk_donors_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_donors_province FOREIGN KEY (province_id)
        REFERENCES provinces (id) ON DELETE RESTRICT,
    CONSTRAINT fk_donors_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_donors_weight CHECK (weight IS NULL OR weight >= 30.0),
    -- Index justification: Donor matching algorithm filters candidate pool by blood group
    INDEX idx_donors_blood_group (blood_group),
    -- Index justification: Fast retrieval of donors who are marked available
    INDEX idx_donors_availability (availability),
    -- Index justification: Same-city bonus scoring in matching algorithm
    INDEX idx_donors_city (city_id),
    -- Index justification: Cooldown calculation relies on last_donation_date
    INDEX idx_donors_last_donation (last_donation_date)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 5. Table: hospitals
-- Healthcare facilities and blood banks authorized to issue emergency requests.
-- -----------------------------------------------------------------------------
CREATE TABLE hospitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    hospital_name VARCHAR(200) NOT NULL,
    registration_number VARCHAR(100) NOT NULL,
    province_id BIGINT NOT NULL,
    city_id BIGINT NOT NULL,
    complete_address VARCHAR(500) NULL,
    contact_number VARCHAR(25) NULL,
    licence_file VARCHAR(255) NULL,
    trust_score INT NOT NULL DEFAULT 100,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_hospitals_user UNIQUE (user_id),
    CONSTRAINT uk_hospitals_reg_no UNIQUE (registration_number),
    CONSTRAINT fk_hospitals_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_hospitals_province FOREIGN KEY (province_id)
        REFERENCES provinces (id) ON DELETE RESTRICT,
    CONSTRAINT fk_hospitals_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hospitals_trust_score CHECK (trust_score >= 0 AND trust_score <= 100),
    -- Index justification: Admin verification queues filter unverified hospitals
    INDEX idx_hospitals_verified (verified),
    -- Index justification: Location-based hospital queries
    INDEX idx_hospitals_city (city_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 6. Table: blood_requests
-- Emergency blood transfusion requests created by accredited hospitals.
-- -----------------------------------------------------------------------------
CREATE TABLE blood_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    blood_group ENUM('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE') NOT NULL,
    units_required INT NOT NULL,
    units_fulfilled INT NOT NULL DEFAULT 0,
    urgency ENUM('CRITICAL', 'URGENT', 'NORMAL') NOT NULL,
    status ENUM('PENDING', 'PENDING_ADMIN_APPROVAL', 'APPROVED', 'MATCHING', 'MATCHING_IN_PROGRESS', 'PARTIALLY_FULFILLED', 'FULFILLED', 'REJECTED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
    required_by DATETIME NOT NULL,
    province_id BIGINT NOT NULL,
    city_id BIGINT NOT NULL,
    hospital_notes VARCHAR(1000) NULL,
    admin_notes VARCHAR(1000) NULL,
    approved_at DATETIME NULL,
    approved_by VARCHAR(120) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_blood_requests_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE RESTRICT,
    CONSTRAINT fk_blood_requests_province FOREIGN KEY (province_id)
        REFERENCES provinces (id) ON DELETE RESTRICT,
    CONSTRAINT fk_blood_requests_city FOREIGN KEY (city_id)
        REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT chk_blood_requests_units CHECK (units_required >= 1 AND units_fulfilled >= 0 AND units_fulfilled <= units_required),
    -- Index justification: Hospital dashboard queries all requests by hospital ID
    INDEX idx_blood_requests_hospital (hospital_id),
    -- Index justification: Public active emergencies board and schedulers filter by status
    INDEX idx_blood_requests_status (status),
    -- Index justification: Schedulers query past-due requests using required_by
    INDEX idx_blood_requests_required_by (required_by),
    -- Index justification: Matching engine searches compatible requests by blood group
    INDEX idx_blood_requests_blood_group (blood_group)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 7. Table: request_matches
-- Individual donor-to-request match evaluations and response lifecycle.
-- -----------------------------------------------------------------------------
CREATE TABLE request_matches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    match_score INT NOT NULL,
    status ENUM('PENDING', 'INVITED', 'ACCEPTED', 'REJECTED', 'NO_RESPONSE', 'CANCELLED', 'COMPLETED', 'CONFIRMED', 'EXPIRED', 'SCHEDULED', 'ARRIVED', 'DONATION_IN_PROGRESS', 'DONATED', 'NO_SHOW') NOT NULL DEFAULT 'PENDING',
    notified_at DATETIME NULL,
    response_at DATETIME NULL,
    donor_notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Enforce CEP mandatory composite unique constraint: one match per donor and request
    CONSTRAINT uk_matches_request_donor UNIQUE (request_id, donor_id),
    CONSTRAINT fk_matches_request FOREIGN KEY (request_id)
        REFERENCES blood_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_donor FOREIGN KEY (donor_id)
        REFERENCES donors (id) ON DELETE CASCADE,
    -- Index justification: Donor matching inbox filters matches for logged-in donor
    INDEX idx_matches_donor (donor_id),
    -- Index justification: Hospital coordinates matches belonging to a blood request
    INDEX idx_matches_request (request_id),
    -- Index justification: Filtering matches by status (e.g. ACCEPTED donors)
    INDEX idx_matches_status (status)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 8. Table: donations
-- Completed blood donations confirmed by hospitals or verified by administrators.
-- -----------------------------------------------------------------------------
CREATE TABLE donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    blood_group ENUM('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE') NOT NULL,
    units_donated INT NOT NULL DEFAULT 1,
    donation_date DATE NOT NULL,
    scheduled_appointment_time DATETIME NULL,
    status ENUM('SCHEDULED', 'DONOR_ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    verified_by_user_id BIGINT NULL,
    clinical_notes VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_donations_match UNIQUE (match_id),
    CONSTRAINT fk_donations_match FOREIGN KEY (match_id)
        REFERENCES request_matches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id)
        REFERENCES donors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_donations_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE RESTRICT,
    CONSTRAINT fk_donations_verifier FOREIGN KEY (verified_by_user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_donations_units CHECK (units_donated >= 1),
    -- Index justification: Donor donation history query
    INDEX idx_donations_donor (donor_id),
    -- Index justification: Hospital donation management query
    INDEX idx_donations_hospital (hospital_id),
    -- Index justification: Filter completed donations
    INDEX idx_donations_status (status)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 9. Table: blood_inventory
-- Hospital-specific blood bank stock balances by blood group.
-- -----------------------------------------------------------------------------
CREATE TABLE blood_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    blood_group ENUM('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE') NOT NULL,
    units_available INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Prevent duplicate blood group records for the same hospital
    CONSTRAINT uk_inventory_hospital_group UNIQUE (hospital_id, blood_group),
    CONSTRAINT fk_inventory_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_units CHECK (units_available >= 0),
    -- Index justification: Fast lookup of hospital stock by hospital ID
    INDEX idx_inventory_hospital (hospital_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 10. Table: blood_stock_entries
-- Itemized audit trail for all blood stock increments, dispatches, and discards.
-- -----------------------------------------------------------------------------
CREATE TABLE blood_stock_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    blood_group ENUM('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE') NOT NULL,
    units INT NOT NULL,
    entry_type ENUM('DONATION_ADD', 'DIRECT_INTAKE', 'ISSUED_TO_PATIENT', 'TRANSFERRED', 'DISCARDED_EXPIRED') NOT NULL,
    donation_id BIGINT NULL,
    notes VARCHAR(500) NULL,
    logged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_entries_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_entries_donation FOREIGN KEY (donation_id)
        REFERENCES donations (id) ON DELETE SET NULL,
    INDEX idx_stock_entries_hospital (hospital_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 11. Table: notifications
-- In-app notifications delivered to donors, hospitals, and administrators.
-- -----------------------------------------------------------------------------
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    notification_type ENUM('BLOOD_REQUEST', 'DONOR_MATCH', 'DONATION_UPDATE', 'DONATION_REMINDER', 'MESSAGE', 'SYSTEM_ALERT') NOT NULL,
    notification_status ENUM('UNREAD', 'READ') NOT NULL DEFAULT 'UNREAD',
    reference_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    -- Index justification: User notification bell queries unread notifications for user
    INDEX idx_notifications_user_status (user_id, notification_status)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 12. Table: conversations & messages
-- Direct real-time messaging between donors, hospitals, and coordinating admins.
-- -----------------------------------------------------------------------------
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blood_request_id BIGINT NULL,
    request_match_id BIGINT NULL,
    donor_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    status ENUM('OPEN', 'CLOSED', 'ARCHIVED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversations_request FOREIGN KEY (blood_request_id)
        REFERENCES blood_requests (id) ON DELETE SET NULL,
    CONSTRAINT fk_conversations_donor FOREIGN KEY (donor_id)
        REFERENCES donors (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_hospital FOREIGN KEY (hospital_id)
        REFERENCES hospitals (id) ON DELETE CASCADE,
    INDEX idx_conversations_donor (donor_id),
    INDEX idx_conversations_hospital (hospital_id)
) ENGINE=InnoDB;

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id)
        REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_messages_conversation (conversation_id),
    INDEX idx_messages_recipient_read (recipient_id, is_read)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 13. Table: audit_logs
-- Immutable tamper-evident security audit trail for all sensitive operations.
-- -----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    action ENUM('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'VERIFY', 'STATUS_CHANGE', 'REVEAL_CONTACT', 'EXPIRE_REQUEST', 'DONATION_RECORDED', 'INVENTORY_UPDATE', 'TRUST_SCORE_UPDATE') NOT NULL,
    entity_name VARCHAR(100) NULL,
    entity_id VARCHAR(100) NULL,
    old_value VARCHAR(1000) NULL,
    new_value VARCHAR(1000) NULL,
    description VARCHAR(2000) NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id)
        REFERENCES users (id) ON DELETE SET NULL,
    -- Index justification: Compliance surveillance filters logs by action and date range
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_created_at (created_at),
    INDEX idx_audit_logs_entity (entity_name, entity_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- 14. Table: settings
-- Configurable platform governance parameters (e.g. cooldown period).
-- -----------------------------------------------------------------------------
CREATE TABLE settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_settings_key UNIQUE (setting_key),
    INDEX idx_settings_key (setting_key)
) ENGINE=InnoDB;

-- =============================================================================
-- MANDATORY DATABASE FEATURES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- (a) Required VIEW: v_eligible_donors
-- Exposes active, verified donors who meet the basic medical requirements
-- (Age 18-60, weight >= 50 kg, availability = AVAILABLE, account active).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_eligible_donors AS
SELECT 
    d.id AS donor_id,
    u.id AS user_id,
    u.full_name,
    u.email,
    d.blood_group,
    d.gender,
    d.date_of_birth,
    TIMESTAMPDIFF(YEAR, d.date_of_birth, CURDATE()) AS age,
    d.weight,
    d.last_donation_date,
    COALESCE(DATEDIFF(CURDATE(), d.last_donation_date), 9999) AS days_since_last_donation,
    p.name AS province_name,
    c.name AS city_name,
    c.id AS city_id
FROM donors d
JOIN users u ON d.user_id = u.id
JOIN provinces p ON d.province_id = p.id
JOIN cities c ON d.city_id = c.id
WHERE u.enabled = TRUE
  AND u.account_status = 'ACTIVE'
  AND d.verified = TRUE
  AND d.availability = 'AVAILABLE'
  AND d.weight >= 50.0
  AND TIMESTAMPDIFF(YEAR, d.date_of_birth, CURDATE()) BETWEEN 18 AND 60;

-- -----------------------------------------------------------------------------
-- (b) Required TRIGGER: trg_after_donation_insert_update_donor
-- Automatically synchronizes donors.last_donation_date upon inserting an
-- approved/completed donation record.
-- -----------------------------------------------------------------------------
DELIMITER //

CREATE TRIGGER trg_after_donation_insert_update_donor
AFTER INSERT ON donations
FOR EACH ROW
BEGIN
    IF NEW.status = 'COMPLETED' THEN
        UPDATE donors
        SET last_donation_date = NEW.donation_date,
            updated_at = NOW()
        WHERE id = NEW.donor_id;
    END IF;
END //

CREATE TRIGGER trg_after_donation_update_donor
AFTER UPDATE ON donations
FOR EACH ROW
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        UPDATE donors
        SET last_donation_date = NEW.donation_date,
            updated_at = NOW()
        WHERE id = NEW.donor_id;
    END IF;
END //

DELIMITER ;

-- -----------------------------------------------------------------------------
-- (c) Explicit SQL Transaction Example
-- Demonstrates atomic unit fulfillment and blood inventory increment.
-- -----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE blood_requests SET units_fulfilled = units_fulfilled + 1 WHERE id = 1;
-- INSERT INTO blood_stock_entries (hospital_id, blood_group, units, entry_type, donation_id, logged_at)
-- VALUES (1, 'O_POSITIVE', 1, 'DONATION_ADD', 1, NOW());
-- UPDATE blood_inventory SET units_available = units_available + 1 WHERE hospital_id = 1 AND blood_group = 'O_POSITIVE';
-- COMMIT;
