-- =============================================================================
-- DonorNexa — Analytical & Executive Reporting Queries
-- DBMS: MySQL 8.0 / MariaDB Compatible
-- Character Set: UTF-8 (utf8mb4)
-- =============================================================================

USE donornexa_db;

-- -----------------------------------------------------------------------------
-- REPORT QUERY 1: Regional Blood Demand vs. Supply & Donor Pool Distribution
-- -----------------------------------------------------------------------------
-- BUSINESS OBJECTIVE:
-- Identifies localized regional shortages by comparing active emergency request units,
-- current hospital blood bank reserve units, and eligible donor counts grouped by
-- city and blood group. Enables administrators and health authorities to prioritize
-- emergency blood drives in deficit zones.
--
-- EXECUTION MECHANICS:
-- 1. Outer joins cities and distinct blood groups to build a complete matrix.
-- 2. Correlated subqueries or aggregated CTEs summarize active request units
--    (status in PENDING, MATCHING, PARTIALLY_FULFILLED).
-- 3. Aggregates current physical stock from blood_inventory.
-- 4. Counts verified, available donors from the v_eligible_donors view.
--
-- INDEX OPTIMIZATION:
-- Uses idx_blood_requests_status, idx_donors_blood_group, and idx_inventory_hospital.
-- -----------------------------------------------------------------------------

SELECT 
    c.name AS city_name,
    p.name AS province_name,
    bg.blood_group,
    COALESCE(req.units_demanded, 0) AS total_units_demanded,
    COALESCE(inv.units_in_stock, 0) AS current_hospital_stock,
    COALESCE(don.eligible_donors_count, 0) AS active_eligible_donors,
    (COALESCE(inv.units_in_stock, 0) + COALESCE(don.eligible_donors_count, 0) - COALESCE(req.units_demanded, 0)) AS net_supply_surplus_deficit,
    CASE 
        WHEN COALESCE(req.units_demanded, 0) > (COALESCE(inv.units_in_stock, 0) + COALESCE(don.eligible_donors_count, 0)) THEN 'CRITICAL DEFICIT'
        WHEN COALESCE(inv.units_in_stock, 0) < 3 THEN 'LOW RESERVE ALERT'
        ELSE 'STABLE'
    END AS supply_status_indicator
FROM cities c
JOIN provinces p ON c.province_id = p.id
CROSS JOIN (
    SELECT 'A_POSITIVE' AS blood_group UNION ALL
    SELECT 'A_NEGATIVE' UNION ALL
    SELECT 'B_POSITIVE' UNION ALL
    SELECT 'B_NEGATIVE' UNION ALL
    SELECT 'AB_POSITIVE' UNION ALL
    SELECT 'AB_NEGATIVE' UNION ALL
    SELECT 'O_POSITIVE' UNION ALL
    SELECT 'O_NEGATIVE'
) bg
LEFT JOIN (
    SELECT 
        city_id, 
        blood_group, 
        SUM(units_required - units_fulfilled) AS units_demanded
    FROM blood_requests
    WHERE status IN ('PENDING', 'MATCHING', 'PARTIALLY_FULFILLED')
    GROUP BY city_id, blood_group
) req ON req.city_id = c.id AND req.blood_group = bg.blood_group
LEFT JOIN (
    SELECT 
        h.city_id, 
        bi.blood_group, 
        SUM(bi.units_available) AS units_in_stock
    FROM blood_inventory bi
    JOIN hospitals h ON bi.hospital_id = h.id
    GROUP BY h.city_id, bi.blood_group
) inv ON inv.city_id = c.id AND inv.blood_group = bg.blood_group
LEFT JOIN (
    SELECT 
        city_id, 
        blood_group, 
        COUNT(*) AS eligible_donors_count
    FROM v_eligible_donors
    WHERE days_since_last_donation >= 90
    GROUP BY city_id, blood_group
) don ON don.city_id = c.id AND don.blood_group = bg.blood_group
WHERE COALESCE(req.units_demanded, 0) > 0 OR COALESCE(inv.units_in_stock, 0) > 0 OR COALESCE(don.eligible_donors_count, 0) > 0
ORDER BY net_supply_surplus_deficit ASC, city_name ASC;


-- -----------------------------------------------------------------------------
-- REPORT QUERY 2: Fulfilment Rate & Donor Response Latency by Urgency Tier
-- -----------------------------------------------------------------------------
-- BUSINESS OBJECTIVE:
-- Evaluates portal operational effectiveness during life-or-death emergencies.
-- Quantifies unit completion percentages and average time (in minutes/hours) taken
-- from blood request broadcast until a volunteer donor formally accepts the match.
--
-- EXECUTION MECHANICS:
-- 1. Aggregates blood_requests by urgency level (CRITICAL, URGENT, NORMAL).
-- 2. Calculates units fulfilled vs. requested ratio as an exact percentage.
-- 3. Joins request_matches to calculate average minutes from request creation
--    to match response_at for all ACCEPTED responses.
--
-- INDEX OPTIMIZATION:
-- Uses idx_blood_requests_status, idx_matches_request, and idx_matches_status.
-- -----------------------------------------------------------------------------

SELECT 
    r.urgency AS urgency_level,
    COUNT(DISTINCT r.id) AS total_requests_count,
    SUM(r.units_required) AS total_units_requested,
    SUM(r.units_fulfilled) AS total_units_fulfilled,
    ROUND(
        (SUM(r.units_fulfilled) * 100.0) / NULLIF(SUM(r.units_required), 0), 2
    ) AS fulfillment_rate_percent,
    COUNT(DISTINCT CASE WHEN r.status = 'FULFILLED' THEN r.id END) AS completely_fulfilled_requests,
    COUNT(DISTINCT CASE WHEN r.status = 'EXPIRED' THEN r.id END) AS expired_unfulfilled_requests,
    ROUND(
        AVG(CASE 
            WHEN m.status IN ('ACCEPTED', 'COMPLETED') AND m.response_at IS NOT NULL 
            THEN TIMESTAMPDIFF(MINUTE, r.created_at, m.response_at) 
            ELSE NULL 
        END), 1
    ) AS avg_donor_response_time_minutes
FROM blood_requests r
LEFT JOIN request_matches m ON r.id = m.request_id
GROUP BY r.urgency
ORDER BY FIELD(r.urgency, 'CRITICAL', 'URGENT', 'NORMAL');


-- -----------------------------------------------------------------------------
-- REPORT QUERY 3: Critical Blood Inventory Depletion & Emergency Shortfall
-- -----------------------------------------------------------------------------
-- BUSINESS OBJECTIVE:
-- Clinical surveillance report for hospital blood bank supervisors.
-- Highlights all blood products where on-hand reserves are near exhaustion (<= 2 units)
-- or zero while pending hospital transfusions are waiting for those exact blood types.
--
-- EXECUTION MECHANICS:
-- 1. Filters blood_inventory for hospitals where units_available <= 2.
-- 2. Correlates open blood requests for the same hospital and blood group.
-- 3. Computes net deficit to trigger immediate inter-hospital transfers.
-- -----------------------------------------------------------------------------

SELECT 
    h.hospital_name,
    c.name AS city_name,
    bi.blood_group,
    bi.units_available AS current_reserve_units,
    COALESCE(pending_req.units_needed, 0) AS pending_emergency_units,
    (bi.units_available - COALESCE(pending_req.units_needed, 0)) AS safety_cushion_units,
    CASE 
        WHEN bi.units_available = 0 AND COALESCE(pending_req.units_needed, 0) > 0 THEN 'LEVEL 1: CRITICAL OUT OF STOCK'
        WHEN bi.units_available = 0 THEN 'LEVEL 2: ZERO RESERVE'
        WHEN bi.units_available <= 2 AND COALESCE(pending_req.units_needed, 0) > bi.units_available THEN 'LEVEL 3: IMMINENT STOCKOUT'
        ELSE 'LEVEL 4: LOW STOCK'
    END AS alert_severity
FROM blood_inventory bi
JOIN hospitals h ON bi.hospital_id = h.id
JOIN cities c ON h.city_id = c.id
LEFT JOIN (
    SELECT 
        hospital_id, 
        blood_group, 
        SUM(units_required - units_fulfilled) AS units_needed
    FROM blood_requests
    WHERE status IN ('PENDING', 'MATCHING', 'PARTIALLY_FULFILLED')
    GROUP BY hospital_id, blood_group
) pending_req ON pending_req.hospital_id = bi.hospital_id AND pending_req.blood_group = bi.blood_group
WHERE bi.units_available <= 2
ORDER BY bi.units_available ASC, pending_emergency_units DESC;


-- -----------------------------------------------------------------------------
-- REPORT QUERY 4: Donor Retention Cohort Segmentation & Recognition Tiers
-- -----------------------------------------------------------------------------
-- BUSINESS OBJECTIVE:
-- Analyzes voluntary donor engagement, retention rates, and gamification tiers
-- (Bronze Lifesaver = 1 donation, Silver Guardian = 2-4 donations, Gold Hero = 5+).
-- Ranks top contributing donors within their respective municipal districts.
--
-- EXECUTION MECHANICS:
-- 1. Aggregates completed donations from the donations table.
-- 2. Calculates lifetime completed donations, last donation date, and cooldown days remaining.
-- 3. Employs window function DENSE_RANK() to compute municipal donor rankings.
-- -----------------------------------------------------------------------------

WITH DonorStats AS (
    SELECT 
        d.id AS donor_id,
        u.full_name AS donor_name,
        u.email,
        d.blood_group,
        c.name AS city_name,
        c.id AS city_id,
        d.last_donation_date,
        COUNT(don.id) AS total_donations,
        COALESCE(DATEDIFF(CURDATE(), d.last_donation_date), 9999) AS days_since_last_donation,
        CASE 
            WHEN COUNT(don.id) >= 5 THEN 'GOLD HERO'
            WHEN COUNT(don.id) >= 2 THEN 'SILVER GUARDIAN'
            WHEN COUNT(don.id) = 1 THEN 'BRONZE LIFESAVER'
            ELSE 'PROSPECTIVE DONOR'
        END AS milestone_badge
    FROM donors d
    JOIN users u ON d.user_id = u.id
    JOIN cities c ON d.city_id = c.id
    LEFT JOIN donations don ON d.id = don.donor_id AND don.status = 'COMPLETED'
    WHERE u.account_status = 'ACTIVE'
    GROUP BY d.id, u.full_name, u.email, d.blood_group, c.name, c.id, d.last_donation_date
)
SELECT 
    donor_id,
    donor_name,
    email,
    blood_group,
    city_name,
    total_donations,
    last_donation_date,
    days_since_last_donation,
    milestone_badge,
    CASE 
        WHEN days_since_last_donation >= 90 THEN 'ELIGIBLE NOW'
        ELSE CONCAT('IN COOLDOWN (', (90 - days_since_last_donation), ' days left)')
    END AS medical_eligibility_status,
    DENSE_RANK() OVER (
        PARTITION BY city_id 
        ORDER BY total_donations DESC, days_since_last_donation ASC
    ) AS city_donor_rank
FROM DonorStats
ORDER BY total_donations DESC, city_name ASC;


-- -----------------------------------------------------------------------------
-- REPORT QUERY 5: Security Compliance & Sensitive Contact Privacy Audit Trail
-- -----------------------------------------------------------------------------
-- BUSINESS OBJECTIVE:
-- Audits access to sensitive personal donor information (e.g. phone numbers revealed
-- via REVEAL_CONTACT action). Detects unauthorized data scraping or anomalous contact
-- inspection frequencies across hospitals and administrative staff.
--
-- EXECUTION MECHANICS:
-- 1. Inspects the audit_logs table filtering for sensitive security actions:
--    REVEAL_CONTACT, STATUS_CHANGE, DELETE, VERIFY, TRUST_SCORE_UPDATE.
-- 2. Aggregates access count per actor and IP address over the past 30 days.
-- 3. Flags actors exceeding typical operational thresholds as privacy risks.
-- -----------------------------------------------------------------------------

SELECT 
    al.actor_id,
    u.full_name AS actor_name,
    u.role AS actor_role,
    al.action AS audit_action,
    al.ip_address,
    COUNT(al.id) AS occurrences_count,
    MIN(al.created_at) AS first_access_time,
    MAX(al.created_at) AS most_recent_access_time,
    GROUP_CONCAT(DISTINCT CONCAT(al.entity_name, '#', al.entity_id) SEPARATOR ', ') AS accessed_entities_sample,
    CASE 
        WHEN COUNT(al.id) > 20 THEN 'ANOMALOUS ACTIVITY ALERT: HIGH VOLUME'
        WHEN COUNT(DISTINCT al.ip_address) > 3 THEN 'SECURITY ALERT: MULTI-IP CONCURRENT ACCESS'
        ELSE 'NORMAL AUDITED ACCESS'
    END AS compliance_risk_assessment
FROM audit_logs al
LEFT JOIN users u ON al.actor_id = u.id
WHERE al.action IN ('REVEAL_CONTACT', 'STATUS_CHANGE', 'DELETE', 'VERIFY', 'TRUST_SCORE_UPDATE')
  AND al.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY al.actor_id, u.full_name, u.role, al.action, al.ip_address
ORDER BY occurrences_count DESC, most_recent_access_time DESC;
