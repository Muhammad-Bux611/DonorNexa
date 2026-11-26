const fs = require('fs');
const path = require('path');

function findInFile(filePath, regex) {
  if (!fs.existsSync(filePath)) return null;
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    if (regex.test(lines[i])) {
      return { line: i + 1, text: lines[i].trim() };
    }
  }
  return null;
}

const targets = [
  { module: "Security & Auth", file: "src/main/java/com/donornexa/config/SecurityConfig.java", regex: /BCryptPasswordEncoder/, feature: "BCrypt Password Encoder Bean" },
  { module: "Security & Auth", file: "src/main/java/com/donornexa/config/SecurityConfig.java", regex: /securityFilterChain/, feature: "Security Filter Chain & URL Role Boundaries" },
  { module: "Security & Auth", file: "src/main/java/com/donornexa/config/RoleBasedSuccessHandler.java", regex: /onAuthenticationSuccess/, feature: "Post-Login Role Router (Admin/Hospital/Donor)" },
  { module: "Security & Auth", file: "src/main/java/com/donornexa/config/AdminPasswordChangeInterceptor.java", regex: /preHandle/, feature: "Admin Mandatory First-Login Password Change" },
  { module: "Security & Auth", file: "src/main/java/com/donornexa/auth/service/AuthService.java", regex: /register/, feature: "Donor & Hospital Account Registration" },
  { module: "Security & Auth", file: "src/main/java/com/donornexa/auth/service/CustomUserDetailsService.java", regex: /loadUserByUsername/, feature: "User Authentication & Authorities Loading" },
  { module: "Audit & Logs", file: "src/main/java/com/donornexa/audit/entity/AuditLog.java", regex: /class AuditLog/, feature: "AuditLog Entity & Table Indexes" },
  { module: "Audit & Logs", file: "src/main/java/com/donornexa/audit/repository/AuditLogRepository.java", regex: /LEFT JOIN FETCH a\.actor/, feature: "AuditLog JOIN FETCH Query (N+1 Solution)" },
  { module: "Audit & Logs", file: "src/main/java/com/donornexa/admin/service/AdminAuditLogService.java", regex: /searchAuditLogs/, feature: "Audit Log Search & Filter Service" },
  { module: "Audit & Logs", file: "src/main/java/com/donornexa/admin/controller/AdminAuditLogController.java", regex: /viewAuditLogs/, feature: "Audit Log Controller & Clamped Pagination" },
  { module: "Blood Request", file: "src/main/java/com/donornexa/bloodrequest/service/BloodRequestAutoExpiryScheduler.java", regex: /sweepExpiredBloodRequests/, feature: "60-Second Auto-Expiry Background Scheduler" },
  { module: "Blood Request", file: "src/main/java/com/donornexa/hospital/service/HospitalBloodRequestService.java", regex: /createRequest|createBloodRequest/, feature: "Blood Request Creation & Validation" },
  { module: "Blood Request", file: "src/main/java/com/donornexa/bloodrequest/repository/BloodRequestRepository.java", regex: /findByStatusInAndRequiredByBefore/, feature: "Overdue Expiry Search Query" },
  { module: "Blood Request", file: "src/main/java/com/donornexa/bloodrequest/entity/BloodRequest.java", regex: /class BloodRequest/, feature: "BloodRequest Entity & Urgency Enum Mapping" },
  { module: "Matching Engine", file: "src/main/java/com/donornexa/matching/service/BloodCompatibilityService.java", regex: /RECIPIENT_TO_DONORS\.put/, feature: "ABO/Rh Blood Compatibility Matrix (EnumMap/EnumSet)" },
  { module: "Matching Engine", file: "src/main/java/com/donornexa/matching/service/BloodCompatibilityService.java", regex: /isCompatible/, feature: "Blood Compatibility Evaluation Method" },
  { module: "Matching Engine", file: "src/main/java/com/donornexa/matching/service/DonorMatchingService.java", regex: /findMatches|evaluateMatches|coordinateMatches/, feature: "Automated Geo-Donor Matching Engine" },
  { module: "Donor Management", file: "src/main/java/com/donornexa/donor/service/DonorEligibilityService.java", regex: /checkEligibility|isEligible|evaluateEligibility/, feature: "56-Day Cooldown & Medical Eligibility Rule" },
  { module: "Donor Management", file: "src/main/java/com/donornexa/donor/service/DonorService.java", regex: /updateAvailability|toggleAvailability/, feature: "Donor Availability Status Toggle" },
  { module: "Donor Management", file: "src/main/java/com/donornexa/donor/entity/Donor.java", regex: /class Donor/, feature: "Donor Entity & Medical Attributes" },
  { module: "Inventory & FEFO", file: "src/main/java/com/donornexa/hospital/service/HospitalInventoryService.java", regex: /issueStock/, feature: "First-Expired, First-Out (FEFO) Stock Issuance" },
  { module: "Inventory & FEFO", file: "src/main/java/com/donornexa/hospital/service/HospitalInventoryService.java", regex: /addDirectStock/, feature: "Direct Stock Intake & Shelf-Life Calculation" },
  { module: "Inventory & FEFO", file: "src/main/java/com/donornexa/inventory/entity/BloodStockEntry.java", regex: /class BloodStockEntry/, feature: "BloodStockEntry Entity (Batch & Expiry Dates)" },
  { module: "Donation Records", file: "src/main/java/com/donornexa/hospital/service/HospitalDonationService.java", regex: /recordDonation|verifyDonation/, feature: "Verify & Record Completed Donation" },
  { module: "Messaging", file: "src/main/java/com/donornexa/messaging/service/MessagingService.java", regex: /sendMessage/, feature: "Two-Way Chat Messaging & IDOR Security Check" },
  { module: "Notifications", file: "src/main/java/com/donornexa/notification/service/NotificationService.java", regex: /createNotification|sendNotification/, feature: "In-App Notification Dispatch" },
  { module: "Badges", file: "src/main/java/com/donornexa/badge/service/DonationBadgeService.java", regex: /evaluateAndAwardBadges|awardBadge|checkBadges/, feature: "Milestone Badges (Bronze 1, Silver 5, Gold 10)" },
  { module: "Location", file: "src/main/java/com/donornexa/location/controller/LocationController.java", regex: /getCitiesByProvince|getProvinces/, feature: "Cascading Dropdowns REST API" },
  { module: "Utilities & Privacy", file: "src/main/java/com/donornexa/common/util/PhoneMaskingUtil.java", regex: /maskPhoneNumber/, feature: "Donor Phone Privacy Masking (+92-300-***1001)" },
  { module: "Utilities & Privacy", file: "src/main/java/com/donornexa/common/service/FileUploadService.java", regex: /JPEG_MAGIC/, feature: "Deep Binary Magic Bytes Validation" },
  { module: "Database", file: "sql/schema.sql", regex: /CREATE TABLE/, feature: "14 Normalized Relational 3NF Tables" },
  { module: "Database", file: "sql/views.sql", regex: /v_eligible_donors/, feature: "v_eligible_donors Database View" },
  { module: "Database", file: "sql/triggers.sql", regex: /CREATE TRIGGER/, feature: "Automated Last Donation Date Trigger" }
];

const results = [];
targets.forEach(t => {
  const res = findInFile(t.file, t.regex);
  if (res) {
    results.push({
      module: t.module,
      feature: t.feature,
      file: t.file,
      className: path.basename(t.file),
      line: res.line,
      code: res.text
    });
  } else {
    // try looser search
    results.push({
      module: t.module,
      feature: t.feature,
      file: t.file,
      className: path.basename(t.file),
      line: 'N/A',
      code: 'Review file'
    });
  }
});

console.log(JSON.stringify(results, null, 2));
