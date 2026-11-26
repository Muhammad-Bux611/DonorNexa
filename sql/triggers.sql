-- =============================================================================
-- DonorNexa — Database Triggers
-- DBMS: MySQL 8.0 / MariaDB Compatible
-- =============================================================================

USE donornexa_db;

DELIMITER //

-- -----------------------------------------------------------------------------
-- Required TRIGGER: trg_after_donation_insert_update_donor
-- Automatically synchronizes donors.last_donation_date upon inserting an
-- approved/completed donation record.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_after_donation_insert_update_donor //

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

-- -----------------------------------------------------------------------------
-- Additional TRIGGER: trg_after_donation_update_donor
-- Automatically synchronizes donors.last_donation_date when an existing scheduled
-- or in-progress donation transitions to 'COMPLETED'.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_after_donation_update_donor //

CREATE TRIGGER trg_after_donation_update_donor
AFTER UPDATE ON donations
FOR EACH ROW
BEGIN
    IF NEW.status = 'COMPLETED' AND (OLD.status IS NULL OR OLD.status != 'COMPLETED') THEN
        UPDATE donors
        SET last_donation_date = NEW.donation_date,
            updated_at = NOW()
        WHERE id = NEW.donor_id;
    END IF;
END //

DELIMITER ;
