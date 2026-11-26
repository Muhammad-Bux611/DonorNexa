-- =============================================================================
-- DonorNexa — Database Views
-- DBMS: MySQL 8.0 / MariaDB Compatible
-- =============================================================================

USE donornexa_db;

-- -----------------------------------------------------------------------------
-- Required VIEW: v_eligible_donors
-- Exposes active, verified voluntary donors who meet the basic clinical standards
-- (Age 18-60, weight >= 50 kg, availability = AVAILABLE, account active).
-- Computes age and days since last donation dynamically.
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
