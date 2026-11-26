-- =============================================================================
-- DonorNexa — Comprehensive Seed Data
-- DBMS: MySQL 8.0 / MariaDB Compatible
-- Character Set: UTF-8 (utf8mb4)
-- Default Password for all seeded users: ValidPass123!
-- BCrypt Hash: $2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK
-- =============================================================================

USE donornexa_db;

-- Disable foreign key checks during batch data insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Clean existing data
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE messages;
TRUNCATE TABLE conversations;
TRUNCATE TABLE notifications;
TRUNCATE TABLE blood_stock_entries;
TRUNCATE TABLE blood_inventory;
TRUNCATE TABLE donations;
TRUNCATE TABLE request_matches;
TRUNCATE TABLE blood_requests;
TRUNCATE TABLE hospitals;
TRUNCATE TABLE donors;
TRUNCATE TABLE cities;
TRUNCATE TABLE provinces;
TRUNCATE TABLE settings;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 1. Provinces
-- -----------------------------------------------------------------------------
INSERT INTO provinces (id, name, code) VALUES
(1, 'Sindh', 'SD'),
(2, 'Punjab', 'PB'),
(3, 'Khyber Pakhtunkhwa', 'KP'),
(4, 'Balochistan', 'BA'),
(5, 'Islamabad Capital Territory', 'ICT');

-- -----------------------------------------------------------------------------
-- 2. Cities
-- -----------------------------------------------------------------------------
INSERT INTO cities (id, province_id, name, latitude, longitude) VALUES
(1, 1, 'Karachi', 24.8607, 67.0011),
(2, 1, 'Hyderabad', 25.3960, 68.3578),
(3, 1, 'Sukkur', 27.7052, 68.8574),
(4, 2, 'Lahore', 31.5204, 74.3587),
(5, 2, 'Rawalpindi', 33.5651, 73.0169),
(6, 2, 'Faisalabad', 31.4504, 73.1350),
(7, 3, 'Peshawar', 34.0151, 71.5249),
(8, 4, 'Quetta', 30.1798, 66.9750),
(9, 5, 'Islamabad', 33.6844, 73.0479);

-- -----------------------------------------------------------------------------
-- 3. Settings
-- -----------------------------------------------------------------------------
INSERT INTO settings (id, setting_key, setting_value, description, updated_at) VALUES
(1, 'donor_cooldown_days', '90', 'Mandatory days a donor must wait between whole blood donations', NOW()),
(2, 'emergency_auto_expiry_hours', '24', 'Hours after which unfulfilled emergency blood requests automatically expire', NOW()),
(3, 'system_status', 'ONLINE', 'Portal operational mode (ONLINE, MAINTENANCE)', NOW()),
(4, 'max_critical_requests_per_hospital', '3', 'Anti-misuse quota: Maximum simultaneous open CRITICAL requests allowed per hospital', NOW());

-- -----------------------------------------------------------------------------
-- 4. Users (1 Admin, 6 Hospitals, 32 Donors)
-- Password for all: ValidPass123!
-- -----------------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, phone_number, role, account_status, enabled, failed_login_attempts, lockout_until, created_at) VALUES
-- Admin (ID: 1)
(1, 'admin@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'System Administrator', '+92-300-1110001', 'ADMIN', 'ACTIVE', TRUE, 0, NULL, NOW()),

-- Hospitals (IDs: 2 - 7)
(2, 'hospital.akuh@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Aga Khan University Hospital', '+92-21-34930051', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),
(3, 'hospital.indus@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Indus Hospital & Health Network', '+92-21-35112709', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),
(4, 'hospital.lnh@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Liaquat National Hospital', '+92-21-111456456', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),
(5, 'hospital.chh@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Civil Hospital Hyderabad', '+92-22-9210123', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),
(6, 'hospital.skmch@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Shaukat Khanum Memorial Hospital', '+92-42-35905000', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),
(7, 'hospital.pims@donornexa.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Pakistan Institute of Medical Sciences', '+92-51-9261170', 'HOSPITAL', 'ACTIVE', TRUE, 0, NULL, NOW()),

-- Donors (IDs: 8 - 39)
(8, 'ahmed.khan@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Ahmed Khan', '+92-300-2101001', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(9, 'fatima.tariq@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Fatima Tariq', '+92-301-2101002', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(10, 'bilal.sheikh@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Bilal Sheikh', '+92-302-2101003', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(11, 'zainab.ali@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Zainab Ali', '+92-303-2101004', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(12, 'hamza.rasheed@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Hamza Rasheed', '+92-304-2101005', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(13, 'maryam.nawaz@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Maryam Nawaz', '+92-305-2101006', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(14, 'usman.ghani@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Usman Ghani', '+92-306-2101007', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(15, 'aisha.siddiqua@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Aisha Siddiqua', '+92-307-2101008', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(16, 'umer.farooq@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Umer Farooq', '+92-308-2101009', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(17, 'sana.mir@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Sana Mir', '+92-309-2101010', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(18, 'daniyal.aziz@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Daniyal Aziz', '+92-310-2101011', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(19, 'rabia.basri@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Rabia Basri', '+92-311-2101012', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(20, 'ali.raza@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Ali Raza', '+92-312-2101013', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(21, 'hira.mani@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Hira Mani', '+92-313-2101014', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(22, 'kashif.mehmood@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Kashif Mehmood', '+92-314-2101015', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(23, 'asma.jehangir@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Asma Jehangir', '+92-315-2101016', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(24, 'fahad.mustafa@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Fahad Mustafa', '+92-316-2101017', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(25, 'nimra.ahmed@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Nimra Ahmed', '+92-317-2101018', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(26, 'saad.iqbal@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Saad Iqbal', '+92-318-2101019', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(27, 'urooj.fatima@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Urooj Fatima', '+92-319-2101020', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(28, 'tariq.jamil@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Tariq Jamil', '+92-320-2101021', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(29, 'sidra.tulain@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Sidra Tulain', '+92-321-2101022', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(30, 'waqas.hassan@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Waqas Hassan', '+92-322-2101023', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(31, 'anaya.rehman@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Anaya Rehman', '+92-323-2101024', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(32, 'junaid.khan@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Junaid Khan', '+92-324-2101025', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(33, 'mahnoor.baloch@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Mahnoor Baloch', '+92-325-2101026', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(34, 'shahid.afridi@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Shahid Afridi', '+92-326-2101027', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(35, 'nadia.khan@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Nadia Khan', '+92-327-2101028', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(36, 'imran.nazir@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Imran Nazir', '+92-328-2101029', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(37, 'sadia.imam@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Sadia Imam', '+92-329-2101030', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(38, 'babar.azam@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Babar Azam', '+92-330-2101031', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW()),
(39, 'bismah.maroof@gmail.com', '$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK', 'Bismah Maroof', '+92-331-2101032', 'DONOR', 'ACTIVE', TRUE, 0, NULL, NOW());

-- -----------------------------------------------------------------------------
-- 5. Hospitals
-- -----------------------------------------------------------------------------
INSERT INTO hospitals (id, user_id, hospital_name, registration_number, province_id, city_id, complete_address, contact_number, licence_file, trust_score, verified, rejection_reason, created_at) VALUES
(1, 2, 'Aga Khan University Hospital', 'SHCC-HOSP-001', 1, 1, 'Stadium Road, Karachi', '+92-21-34930051', 'licence_akuh.pdf', 100, TRUE, NULL, NOW()),
(2, 3, 'Indus Hospital & Health Network', 'SHCC-HOSP-002', 1, 1, 'Plot C-76, Sector 31/5, Korangi, Karachi', '+92-21-35112709', 'licence_indus.pdf', 95, TRUE, NULL, NOW()),
(3, 4, 'Liaquat National Hospital', 'SHCC-HOSP-003', 1, 1, 'National Stadium Road, Karachi', '+92-21-111456456', 'licence_lnh.pdf', 90, TRUE, NULL, NOW()),
(4, 5, 'Civil Hospital Hyderabad', 'SHCC-HOSP-004', 1, 2, 'Hospital Road, Saddar, Hyderabad', '+92-22-9210123', 'licence_chh.pdf', 85, TRUE, NULL, NOW()),
(5, 6, 'Shaukat Khanum Memorial Hospital', 'PHC-HOSP-005', 2, 4, '7A Block R-3, Johar Town, Lahore', '+92-42-35905000', 'licence_skmch.pdf', 100, TRUE, NULL, NOW()),
(6, 7, 'Pakistan Institute of Medical Sciences', 'IHRA-HOSP-006', 5, 9, 'G-8/3, Islamabad', '+92-51-9261170', 'licence_pims.pdf', 95, TRUE, NULL, NOW());

-- -----------------------------------------------------------------------------
-- 6. Donors
-- Covering all 8 blood groups, realistic ages (18-60), weights (>= 50kg),
-- and availability statuses.
-- -----------------------------------------------------------------------------
INSERT INTO donors (id, user_id, blood_group, date_of_birth, gender, weight, province_id, city_id, complete_address, last_donation_date, availability, photo, verified, rejection_reason, created_at) VALUES
-- A_POSITIVE (Donors 1-4)
(1, 8, 'A_POSITIVE', '1995-04-12', 'MALE', 72.5, 1, 1, 'Gulshan-e-Iqbal Block 5, Karachi', '2026-06-15', 'AVAILABLE', 'donor_1.jpg', TRUE, NULL, NOW()),
(2, 9, 'A_POSITIVE', '1998-09-22', 'FEMALE', 58.0, 1, 1, 'Clifton Block 2, Karachi', NULL, 'AVAILABLE', 'donor_2.jpg', TRUE, NULL, NOW()),
(3, 10, 'A_POSITIVE', '2001-01-30', 'MALE', 68.0, 1, 2, 'Latifabad Unit 7, Hyderabad', '2026-08-01', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(4, 11, 'A_POSITIVE', '1993-11-15', 'FEMALE', 62.0, 2, 4, 'Model Town, Lahore', '2026-10-25', 'AVAILABLE', NULL, TRUE, NULL, NOW()), -- in cooldown

-- A_NEGATIVE (Donors 5-7)
(5, 12, 'A_NEGATIVE', '1990-07-08', 'MALE', 80.0, 1, 1, 'PECHS Block 6, Karachi', '2026-05-10', 'AVAILABLE', 'donor_5.jpg', TRUE, NULL, NOW()),
(6, 13, 'A_NEGATIVE', '1997-03-14', 'FEMALE', 55.5, 2, 4, 'Gulberg III, Lahore', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(7, 14, 'A_NEGATIVE', '2000-12-05', 'MALE', 74.0, 5, 9, 'F-10/2, Islamabad', '2026-07-20', 'AVAILABLE', NULL, TRUE, NULL, NOW()),

-- B_POSITIVE (Donors 8-11)
(8, 15, 'B_POSITIVE', '1992-02-18', 'FEMALE', 60.0, 1, 1, 'North Nazimabad Block H, Karachi', '2026-04-05', 'AVAILABLE', 'donor_8.jpg', TRUE, NULL, NOW()),
(9, 16, 'B_POSITIVE', '1988-08-27', 'MALE', 85.0, 1, 2, 'Qasimabad Phase 1, Hyderabad', '2026-06-30', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(10, 17, 'B_POSITIVE', '1996-10-10', 'FEMALE', 57.0, 2, 4, 'DHA Phase 5, Lahore', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(11, 18, 'B_POSITIVE', '2002-05-25', 'MALE', 69.5, 1, 3, 'Military Road, Sukkur', '2026-11-01', 'TEMPORARILY_UNAVAILABLE', NULL, TRUE, NULL, NOW()),

-- B_NEGATIVE (Donors 12-14)
(12, 19, 'B_NEGATIVE', '1994-06-19', 'FEMALE', 53.0, 1, 1, 'DHA Phase 6, Karachi', '2026-03-12', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(13, 20, 'B_NEGATIVE', '1991-01-11', 'MALE', 78.0, 2, 4, 'Cantt, Lahore', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(14, 21, 'B_NEGATIVE', '1999-08-04', 'FEMALE', 56.0, 5, 9, 'G-11/3, Islamabad', '2026-07-02', 'AVAILABLE', NULL, TRUE, NULL, NOW()),

-- AB_POSITIVE (Donors 15-18)
(15, 22, 'AB_POSITIVE', '1987-12-01', 'MALE', 82.0, 1, 1, 'Bahadurabad, Karachi', '2026-05-18', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(16, 23, 'AB_POSITIVE', '1995-09-09', 'FEMALE', 64.0, 1, 2, 'Saddar, Hyderabad', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(17, 24, 'AB_POSITIVE', '2001-04-14', 'MALE', 71.0, 2, 4, 'Johar Town, Lahore', '2026-08-14', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(18, 25, 'AB_POSITIVE', '1998-11-28', 'FEMALE', 59.0, 2, 5, 'Satellite Town, Rawalpindi', '2026-10-15', 'AVAILABLE', NULL, TRUE, NULL, NOW()),

-- AB_NEGATIVE (Donors 19-20 - Rare)
(19, 26, 'AB_NEGATIVE', '1993-03-31', 'MALE', 76.5, 1, 1, 'Federal B Area Block 14, Karachi', '2026-04-20', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(20, 27, 'AB_NEGATIVE', '2000-07-17', 'FEMALE', 54.0, 2, 4, 'Wapda Town, Lahore', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),

-- O_POSITIVE (Donors 21-26 - Universal Red Cell Donor for Rh+)
(21, 28, 'O_POSITIVE', '1985-05-05', 'MALE', 88.0, 1, 1, 'KDA Scheme 1, Karachi', '2026-06-01', 'AVAILABLE', 'donor_21.jpg', TRUE, NULL, NOW()),
(22, 29, 'O_POSITIVE', '1996-01-20', 'FEMALE', 61.0, 1, 1, 'Malir Cantt, Karachi', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(23, 30, 'O_POSITIVE', '1992-10-03', 'MALE', 75.0, 1, 2, 'Citizen Colony, Hyderabad', '2026-07-11', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(24, 31, 'O_POSITIVE', '2003-02-14', 'FEMALE', 52.0, 2, 4, 'Faisal Town, Lahore', '2026-05-22', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(25, 32, 'O_POSITIVE', '1994-08-16', 'MALE', 83.0, 5, 9, 'I-8/2, Islamabad', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(26, 33, 'O_POSITIVE', '1997-12-24', 'FEMALE', 58.5, 1, 3, 'Bunder Road, Sukkur', '2026-08-05', 'AVAILABLE', NULL, TRUE, NULL, NOW()),

-- O_NEGATIVE (Donors 27-32 - Universal Red Cell Donor for All)
(27, 34, 'O_NEGATIVE', '1989-11-02', 'MALE', 79.0, 1, 1, 'Gulistan-e-Jauhar Block 12, Karachi', '2026-06-18', 'AVAILABLE', 'donor_27.jpg', TRUE, NULL, NOW()),
(28, 35, 'O_NEGATIVE', '1998-04-09', 'FEMALE', 55.0, 1, 1, 'Defence Phase 2, Karachi', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(29, 36, 'O_NEGATIVE', '1991-08-30', 'MALE', 81.0, 1, 2, 'Auto Bhan Road, Hyderabad', '2026-07-15', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(30, 37, 'O_NEGATIVE', '1995-02-27', 'FEMALE', 56.5, 2, 4, 'Garden Town, Lahore', '2026-04-10', 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(31, 38, 'O_NEGATIVE', '2001-10-15', 'MALE', 73.0, 5, 9, 'E-11/3, Islamabad', NULL, 'AVAILABLE', NULL, TRUE, NULL, NOW()),
(32, 39, 'O_NEGATIVE', '1999-05-08', 'FEMALE', 53.5, 2, 5, 'Chaklala Scheme 3, Rawalpindi', '2026-09-20', 'TEMPORARILY_UNAVAILABLE', NULL, TRUE, NULL, NOW());

-- -----------------------------------------------------------------------------
-- 7. Blood Requests (22 Requests across urgencies and statuses)
-- -----------------------------------------------------------------------------
INSERT INTO blood_requests (id, hospital_id, blood_group, units_required, units_fulfilled, urgency, status, required_by, province_id, city_id, hospital_notes, admin_notes, approved_at, approved_by, version, created_at) VALUES
-- Active Emergencies for Karachi
(1, 1, 'O_NEGATIVE', 2, 1, 'CRITICAL', 'PARTIALLY_FULFILLED', DATE_ADD(NOW(), INTERVAL 4 HOUR), 1, 1, 'Immediate trauma resuscitation in ER - ICU Bed 4', 'Approved immediately due to trauma', NOW(), 'admin@donornexa.com', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 1, 'A_POSITIVE', 3, 0, 'CRITICAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 6 HOUR), 1, 1, 'Open heart coronary bypass surgery patient', 'Verified with hospital blood bank director', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(3, 2, 'B_POSITIVE', 2, 0, 'CRITICAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 5 HOUR), 1, 1, 'Post-partum hemorrhage emergency in maternity wing', 'High priority maternity case', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, 3, 'AB_NEGATIVE', 1, 0, 'CRITICAL', 'PENDING', DATE_ADD(NOW(), INTERVAL 8 HOUR), 1, 1, 'Severe aplastic anemia crisis, rare blood group required', NULL, NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- Urgent & Normal Requests for Karachi
(5, 1, 'O_POSITIVE', 4, 4, 'URGENT', 'FULFILLED', DATE_ADD(NOW(), INTERVAL 12 HOUR), 1, 1, 'Thalassemia major pediatric patients transfusion cohort', 'Fulfilled via matching donors', NOW(), 'admin@donornexa.com', 4, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 2, 'A_NEGATIVE', 2, 1, 'URGENT', 'PARTIALLY_FULFILLED', DATE_ADD(NOW(), INTERVAL 18 HOUR), 1, 1, 'Elective orthopaedic hip replacement surgery', 'Coordinated with donor', NOW(), 'admin@donornexa.com', 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(7, 3, 'B_NEGATIVE', 2, 0, 'NORMAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 36 HOUR), 1, 1, 'Scheduled oncology chemotherapy supportive care', NULL, NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(8, 2, 'AB_POSITIVE', 2, 2, 'NORMAL', 'FULFILLED', DATE_ADD(NOW(), INTERVAL 20 HOUR), 1, 1, 'Dialysis related severe anemia management', 'Donation confirmed', NOW(), 'admin@donornexa.com', 2, DATE_SUB(NOW(), INTERVAL 40 HOUR)),

-- Hyderabad Requests
(9, 4, 'O_POSITIVE', 3, 0, 'CRITICAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 5 HOUR), 1, 2, 'Massive road traffic accident trauma victims', 'Approved and notified donors in Hyderabad', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(10, 4, 'A_POSITIVE', 2, 1, 'URGENT', 'PARTIALLY_FULFILLED', DATE_ADD(NOW(), INTERVAL 14 HOUR), 1, 2, 'Dengue hemorrhagic fever with critical platelet/RBC loss', NULL, NOW(), 'admin@donornexa.com', 1, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(11, 4, 'B_POSITIVE', 1, 1, 'NORMAL', 'FULFILLED', DATE_ADD(NOW(), INTERVAL 48 HOUR), 1, 2, 'Elective gallbladder surgery blood reserve', NULL, NOW(), 'admin@donornexa.com', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(12, 4, 'O_NEGATIVE', 1, 0, 'URGENT', 'CANCELLED', DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 2, 'Patient transferred to tertiary hospital in Karachi', 'Cancelled by hospital coordinator', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Lahore Requests
(13, 5, 'A_NEGATIVE', 2, 0, 'CRITICAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 7 HOUR), 2, 4, 'Leukemia patient emergency bone marrow conditioning', 'Fast-tracked for matching', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(14, 5, 'O_POSITIVE', 4, 2, 'URGENT', 'PARTIALLY_FULFILLED', DATE_ADD(NOW(), INTERVAL 16 HOUR), 2, 4, 'Pediatric oncology transfusion unit', 'Partial donors accepted', NOW(), 'admin@donornexa.com', 2, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(15, 5, 'B_NEGATIVE', 1, 1, 'NORMAL', 'FULFILLED', DATE_ADD(NOW(), INTERVAL 24 HOUR), 2, 4, 'Routine surgical backup for liver resection', 'Complete', NOW(), 'admin@donornexa.com', 1, DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(16, 5, 'AB_POSITIVE', 2, 0, 'NORMAL', 'PENDING', DATE_ADD(NOW(), INTERVAL 72 HOUR), 2, 4, 'Upcoming vascular reconstructive procedure', NULL, NULL, NULL, 0, DATE_SUB(NOW(), INTERVAL 4 HOUR)),

-- Islamabad Requests
(17, 6, 'O_NEGATIVE', 2, 0, 'CRITICAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 4 HOUR), 5, 9, 'Bomb blast casualty / severe multi-organ trauma', 'Emergency response initiated', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(18, 6, 'A_POSITIVE', 3, 3, 'URGENT', 'FULFILLED', DATE_ADD(NOW(), INTERVAL 12 HOUR), 5, 9, 'Emergency neurosurgical intervention', 'Donations verified', NOW(), 'admin@donornexa.com', 3, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(19, 6, 'B_POSITIVE', 2, 0, 'NORMAL', 'MATCHING', DATE_ADD(NOW(), INTERVAL 48 HOUR), 5, 9, 'Burn unit plasma and RBC replenishment', NULL, NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 12 HOUR)),

-- Expired Historical Requests (for audit & analytics testing)
(20, 1, 'AB_NEGATIVE', 1, 0, 'CRITICAL', 'EXPIRED', DATE_SUB(NOW(), INTERVAL 5 DAY), 1, 1, 'Historical expired emergency request', 'Auto-expired by system scheduler', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(21, 2, 'O_NEGATIVE', 2, 0, 'URGENT', 'EXPIRED', DATE_SUB(NOW(), INTERVAL 3 DAY), 1, 1, 'Historical expired request', 'Auto-expired due to requiredBy passed', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(22, 5, 'B_NEGATIVE', 1, 0, 'NORMAL', 'EXPIRED', DATE_SUB(NOW(), INTERVAL 8 DAY), 2, 4, 'Historical expired non-urgent request', 'Expired by scheduler', NOW(), 'admin@donornexa.com', 0, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- -----------------------------------------------------------------------------
-- 8. Request Matches (Donor matching engine results & response tracking)
-- -----------------------------------------------------------------------------
INSERT INTO request_matches (id, request_id, donor_id, match_score, status, notified_at, response_at, donor_notes, created_at) VALUES
-- Matches for Request 1 (O_NEGATIVE, AKUH Karachi)
(1, 1, 27, 95, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 110 MINUTE), DATE_SUB(NOW(), INTERVAL 95 MINUTE), 'On my way to AKUH blood bank now', DATE_SUB(NOW(), INTERVAL 115 MINUTE)),
(2, 1, 28, 88, 'INVITED', DATE_SUB(NOW(), INTERVAL 110 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 115 MINUTE)),

-- Matches for Request 2 (A_POSITIVE, AKUH Karachi)
(3, 2, 1, 92, 'INVITED', DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(4, 2, 2, 85, 'INVITED', DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(5, 2, 21, 78, 'PENDING', DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),

-- Matches for Request 3 (B_POSITIVE, Indus Karachi)
(6, 3, 8, 94, 'INVITED', DATE_SUB(NOW(), INTERVAL 160 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 170 MINUTE)),
(7, 3, 21, 75, 'REJECTED', DATE_SUB(NOW(), INTERVAL 160 MINUTE), DATE_SUB(NOW(), INTERVAL 140 MINUTE), 'Out of city for 2 days', DATE_SUB(NOW(), INTERVAL 170 MINUTE)),

-- Matches for Request 5 (O_POSITIVE, AKUH Fulfilled)
(8, 5, 21, 95, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 22 HOUR), 'Will donate immediately', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(9, 5, 22, 90, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 22 HOUR), 'Arrived at blood bank', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(10, 5, 27, 85, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 22 HOUR), 'Donated unit', DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(11, 5, 28, 80, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 21 HOUR), 'Donation complete', DATE_SUB(NOW(), INTERVAL 23 HOUR)),

-- Matches for Request 6 (A_NEGATIVE, Indus Karachi)
(12, 6, 5, 96, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 11 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR), 'Can come at 4 PM', DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(13, 6, 27, 82, 'INVITED', DATE_SUB(NOW(), INTERVAL 11 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 11 HOUR)),

-- Matches for Request 9 (O_POSITIVE, Hyderabad)
(14, 9, 23, 96, 'INVITED', DATE_SUB(NOW(), INTERVAL 110 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 115 MINUTE)),
(15, 9, 29, 85, 'INVITED', DATE_SUB(NOW(), INTERVAL 110 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 115 MINUTE)),

-- Matches for Request 10 (A_POSITIVE, Hyderabad)
(16, 10, 3, 95, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), 'Available to donate in Latifabad', DATE_SUB(NOW(), INTERVAL 9 HOUR)),

-- Matches for Request 11 (B_POSITIVE, Hyderabad Fulfilled)
(17, 11, 9, 94, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'Successfully donated', DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- Matches for Request 13 (A_NEGATIVE, Lahore)
(18, 13, 6, 95, 'INVITED', DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(19, 13, 30, 84, 'INVITED', DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),

-- Matches for Request 14 (O_POSITIVE, Lahore)
(20, 14, 24, 95, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 14 HOUR), DATE_SUB(NOW(), INTERVAL 13 HOUR), 'Will be there at 10 AM', DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(21, 14, 30, 85, 'ACCEPTED', DATE_SUB(NOW(), INTERVAL 14 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), 'Confirmed attendance', DATE_SUB(NOW(), INTERVAL 14 HOUR)),

-- Matches for Request 15 (B_NEGATIVE, Lahore Fulfilled)
(22, 15, 13, 96, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 35 HOUR), DATE_SUB(NOW(), INTERVAL 34 HOUR), 'Donated successfully', DATE_SUB(NOW(), INTERVAL 35 HOUR)),

-- Matches for Request 17 (O_NEGATIVE, Islamabad)
(23, 17, 31, 98, 'INVITED', DATE_SUB(NOW(), INTERVAL 80 MINUTE), NULL, NULL, DATE_SUB(NOW(), INTERVAL 85 MINUTE)),

-- Matches for Request 18 (A_POSITIVE, Islamabad Fulfilled)
(24, 18, 7, 85, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Done', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(25, 18, 25, 80, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Done', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(26, 18, 31, 75, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'Done', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- -----------------------------------------------------------------------------
-- 9. Donations (Completed / In-Progress donations)
-- -----------------------------------------------------------------------------
INSERT INTO donations (id, match_id, donor_id, hospital_id, blood_group, units_donated, donation_date, scheduled_appointment_time, status, verified_by_user_id, clinical_notes, created_at) VALUES
-- Fulfilled units for Request 5
(1, 8, 21, 1, 'O_POSITIVE', 1, CURDATE(), DATE_SUB(NOW(), INTERVAL 18 HOUR), 'COMPLETED', 1, 'Screening clear, Hb 14.8 g/dL, 450ml drawn safely', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(2, 9, 22, 1, 'O_POSITIVE', 1, CURDATE(), DATE_SUB(NOW(), INTERVAL 17 HOUR), 'COMPLETED', 1, 'Screening clear, Hb 13.5 g/dL, whole blood donation', DATE_SUB(NOW(), INTERVAL 17 HOUR)),
(3, 10, 27, 1, 'O_NEGATIVE', 1, CURDATE(), DATE_SUB(NOW(), INTERVAL 16 HOUR), 'COMPLETED', 1, 'Universal red cell unit accepted, screening clear', DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(4, 11, 28, 1, 'O_NEGATIVE', 1, CURDATE(), DATE_SUB(NOW(), INTERVAL 15 HOUR), 'COMPLETED', 1, 'Donor arrived on schedule, safe phlebotomy', DATE_SUB(NOW(), INTERVAL 15 HOUR)),

-- Request 11 (Hyderabad)
(5, 17, 9, 4, 'B_POSITIVE', 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'COMPLETED', 1, 'Successful whole blood intake', DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- Request 15 (Lahore)
(6, 22, 13, 5, 'B_NEGATIVE', 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 HOUR), 'COMPLETED', 1, 'Rare unit collected, cross-match positive', DATE_SUB(NOW(), INTERVAL 30 HOUR)),

-- Request 18 (Islamabad)
(7, 24, 7, 6, 'A_NEGATIVE', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED', 1, 'Donated for neurosurgical patient', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 25, 25, 6, 'O_POSITIVE', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED', 1, 'Verified transfusion standard', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9, 26, 31, 6, 'O_NEGATIVE', 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED', 1, 'Safe collection', DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Currently Scheduled / Accepted donations
(10, 1, 27, 1, 'O_NEGATIVE', 1, CURDATE(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'SCHEDULED', NULL, 'Trauma emergency appointment confirmed with donor', DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
(11, 12, 5, 2, 'A_NEGATIVE', 1, CURDATE(), DATE_ADD(NOW(), INTERVAL 4 HOUR), 'SCHEDULED', NULL, 'Orthopedic case blood reservation', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(12, 16, 3, 4, 'A_POSITIVE', 1, CURDATE(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 'SCHEDULED', NULL, 'Appointment booked at Hyderabad Civil blood bank', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(13, 20, 24, 5, 'O_POSITIVE', 1, CURDATE(), DATE_ADD(NOW(), INTERVAL 5 HOUR), 'SCHEDULED', NULL, 'Pediatric oncology transfusion appointment', DATE_SUB(NOW(), INTERVAL 13 HOUR)),
(14, 21, 30, 5, 'O_NEGATIVE', 1, CURDATE(), DATE_ADD(NOW(), INTERVAL 6 HOUR), 'SCHEDULED', NULL, 'Scheduled for afternoon donation slot', DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- -----------------------------------------------------------------------------
-- 10. Blood Inventory (Full stock matrices across 8 blood groups for all 6 hospitals)
-- -----------------------------------------------------------------------------
-- Aga Khan University Hospital (Karachi)
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(1, 'A_POSITIVE', 12, NOW()),
(1, 'A_NEGATIVE', 2, NOW()),
(1, 'B_POSITIVE', 15, NOW()),
(1, 'B_NEGATIVE', 1, NOW()),  -- low stock
(1, 'AB_POSITIVE', 6, NOW()),
(1, 'AB_NEGATIVE', 0, NOW()), -- critical zero stock!
(1, 'O_POSITIVE', 18, NOW()),
(1, 'O_NEGATIVE', 1, NOW());  -- critical low stock

-- Indus Hospital (Karachi)
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(2, 'A_POSITIVE', 8, NOW()),
(2, 'A_NEGATIVE', 1, NOW()),  -- critical low stock
(2, 'B_POSITIVE', 10, NOW()),
(2, 'B_NEGATIVE', 2, NOW()),
(2, 'AB_POSITIVE', 4, NOW()),
(2, 'AB_NEGATIVE', 0, NOW()), -- critical zero stock!
(2, 'O_POSITIVE', 14, NOW()),
(2, 'O_NEGATIVE', 2, NOW());

-- Liaquat National Hospital (Karachi)
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(3, 'A_POSITIVE', 9, NOW()),
(3, 'A_NEGATIVE', 3, NOW()),
(3, 'B_POSITIVE', 7, NOW()),
(3, 'B_NEGATIVE', 1, NOW()),
(3, 'AB_POSITIVE', 5, NOW()),
(3, 'AB_NEGATIVE', 1, NOW()),
(3, 'O_POSITIVE', 11, NOW()),
(3, 'O_NEGATIVE', 2, NOW());

-- Civil Hospital Hyderabad
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(4, 'A_POSITIVE', 4, NOW()),
(4, 'A_NEGATIVE', 0, NOW()), -- critical zero stock!
(4, 'B_POSITIVE', 6, NOW()),
(4, 'B_NEGATIVE', 1, NOW()),
(4, 'AB_POSITIVE', 3, NOW()),
(4, 'AB_NEGATIVE', 0, NOW()), -- critical zero stock!
(4, 'O_POSITIVE', 5, NOW()),
(4, 'O_NEGATIVE', 0, NOW()); -- critical zero stock!

-- Shaukat Khanum Memorial Hospital (Lahore)
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(5, 'A_POSITIVE', 14, NOW()),
(5, 'A_NEGATIVE', 2, NOW()),
(5, 'B_POSITIVE', 16, NOW()),
(5, 'B_NEGATIVE', 3, NOW()),
(5, 'AB_POSITIVE', 7, NOW()),
(5, 'AB_NEGATIVE', 1, NOW()),
(5, 'O_POSITIVE', 20, NOW()),
(5, 'O_NEGATIVE', 3, NOW());

-- Pakistan Institute of Medical Sciences (Islamabad)
INSERT INTO blood_inventory (hospital_id, blood_group, units_available, updated_at) VALUES
(6, 'A_POSITIVE', 11, NOW()),
(6, 'A_NEGATIVE', 3, NOW()),
(6, 'B_POSITIVE', 12, NOW()),
(6, 'B_NEGATIVE', 2, NOW()),
(6, 'AB_POSITIVE', 5, NOW()),
(6, 'AB_NEGATIVE', 1, NOW()),
(6, 'O_POSITIVE', 15, NOW()),
(6, 'O_NEGATIVE', 1, NOW()); -- critical low stock

-- -----------------------------------------------------------------------------
-- 11. Blood Stock Entries (Audit trail for inventory changes)
-- -----------------------------------------------------------------------------
INSERT INTO blood_stock_entries (hospital_id, blood_group, units, entry_type, donation_id, notes, logged_at) VALUES
(1, 'O_POSITIVE', 1, 'DONATION_ADD', 1, 'Donation unit added to inventory', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 'O_POSITIVE', 1, 'DONATION_ADD', 2, 'Donation unit added to inventory', DATE_SUB(NOW(), INTERVAL 17 HOUR)),
(1, 'O_NEGATIVE', 1, 'DONATION_ADD', 3, 'Donation unit added to inventory', DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(1, 'O_NEGATIVE', 1, 'DONATION_ADD', 4, 'Donation unit added to inventory', DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(1, 'O_POSITIVE', 2, 'ISSUED_TO_PATIENT', NULL, 'Dispatched to OR 3 for cardiac surgery', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(4, 'B_POSITIVE', 1, 'DONATION_ADD', 5, 'Donation unit added to inventory', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 'B_NEGATIVE', 1, 'DONATION_ADD', 6, 'Rare B- donor unit collected', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(6, 'A_NEGATIVE', 1, 'DONATION_ADD', 7, 'Neurosurgical blood reserve intake', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 'O_POSITIVE', 1, 'DONATION_ADD', 8, 'Regular donation intake', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(6, 'O_NEGATIVE', 1, 'DONATION_ADD', 9, 'Critical O- intake', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'AB_NEGATIVE', 1, 'DISCARDED_EXPIRED', NULL, 'Unit exceeded 42 days storage viability limit', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- -----------------------------------------------------------------------------
-- 12. Conversations & Messages
-- -----------------------------------------------------------------------------
INSERT INTO conversations (id, blood_request_id, request_match_id, donor_id, hospital_id, status, created_at) VALUES
(1, 1, 1, 27, 1, 'OPEN', DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
(2, 6, 12, 5, 2, 'OPEN', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(3, 10, 16, 3, 4, 'OPEN', DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO messages (conversation_id, sender_id, recipient_id, content, is_read, created_at) VALUES
-- Conversation 1: Donor 27 (User ID: 34) and Hospital AKUH (User ID: 2)
(1, 2, 34, 'Dear Tariq, thank you for responding to our critical O- emergency request. Please proceed to AKUH Main Blood Bank (Ground Floor, Building A).', TRUE, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(1, 34, 2, 'I am in Gulistan-e-Jauhar, starting now. I should reach within 25 minutes. Please have the reception desk informed.', TRUE, DATE_SUB(NOW(), INTERVAL 85 MINUTE)),
(1, 2, 34, 'Security and reception have your name on the emergency priority access list. See you soon!', FALSE, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),

-- Conversation 2: Donor 5 (User ID: 12) and Hospital Indus (User ID: 3)
(2, 3, 12, 'Hello Hamza, thank you for accepting our A- request. What time works best for your donation today?', TRUE, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(2, 12, 3, 'I can arrive today around 4:00 PM after office hours. Is that suitable?', TRUE, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(2, 3, 12, '4:00 PM is confirmed. We will keep a phlebotomy station reserved for you.', TRUE, DATE_SUB(NOW(), INTERVAL 7 HOUR));

-- -----------------------------------------------------------------------------
-- 13. Notifications
-- -----------------------------------------------------------------------------
INSERT INTO notifications (user_id, title, message, notification_type, notification_status, reference_id, created_at) VALUES
(34, 'CRITICAL Blood Match Alert', 'Emergency match: O_NEGATIVE blood required at Aga Khan University Hospital. Patient requires immediate transfusion.', 'DONOR_MATCH', 'READ', 1, DATE_SUB(NOW(), INTERVAL 110 MINUTE)),
(8, 'New Blood Request Match', 'Urgent match: A_POSITIVE blood needed at Aga Khan University Hospital within 6 hours.', 'DONOR_MATCH', 'UNREAD', 2, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(15, 'Critical Emergency Alert', 'Emergency match: B_POSITIVE blood required at Indus Hospital for post-partum hemorrhage case.', 'DONOR_MATCH', 'UNREAD', 3, DATE_SUB(NOW(), INTERVAL 160 MINUTE)),
(2, 'Donor Match Accepted', 'Donor Tariq Jamil (O_NEGATIVE) accepted emergency match request #1. Scheduled arrival in 25 mins.', 'DONATION_UPDATE', 'READ', 1, DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
(3, 'Donor Match Accepted', 'Donor Hamza Rasheed (A_NEGATIVE) accepted urgent match request #6 for 4:00 PM.', 'DONATION_UPDATE', 'READ', 6, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(1, 'New Emergency Request Submitted', 'Hospital AKUH submitted CRITICAL request #1 for O_NEGATIVE blood units.', 'BLOOD_REQUEST', 'READ', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- -----------------------------------------------------------------------------
-- 14. Audit Logs (Tamper-evident system activity log)
-- -----------------------------------------------------------------------------
INSERT INTO audit_logs (actor_id, action, entity_name, entity_id, old_value, new_value, description, ip_address, created_at) VALUES
(1, 'LOGIN', 'User', '1', NULL, NULL, 'System Administrator logged in successfully', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(1, 'VERIFY', 'Hospital', '1', 'verified=false', 'verified=true', 'Aga Khan University Hospital license verified with SHCC', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(2, 'CREATE', 'BloodRequest', '1', NULL, 'status=PENDING, urgency=CRITICAL, bloodGroup=O_NEGATIVE, units=2', 'Emergency blood request created by AKUH coordinator', '192.168.1.101', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 'STATUS_CHANGE', 'BloodRequest', '1', 'status=PENDING', 'status=MATCHING', 'Administrator approved emergency request and activated matching engine', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 115 MINUTE)),
(34, 'UPDATE', 'RequestMatch', '1', 'status=INVITED', 'status=ACCEPTED', 'Donor accepted emergency match request via mobile portal', '10.0.0.45', DATE_SUB(NOW(), INTERVAL 95 MINUTE)),
(2, 'REVEAL_CONTACT', 'Donor', '27', NULL, NULL, 'AKUH staff viewed contact phone number for emergency coordination of Donor Tariq Jamil', '192.168.1.101', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(1, 'DONATION_RECORDED', 'Donation', '1', NULL, 'units=1, donor=21, hospital=1', 'Donation completed and confirmed by blood bank director', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 'INVENTORY_UPDATE', 'BloodInventory', '1', 'units=17', 'units=18', 'Blood stock incremented automatically following donation confirmation', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(1, 'EXPIRE_REQUEST', 'BloodRequest', '20', 'status=PENDING', 'status=EXPIRED', 'Automated scheduler expired unfulfilled emergency request #20 after requiredBy deadline', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 5 DAY));
