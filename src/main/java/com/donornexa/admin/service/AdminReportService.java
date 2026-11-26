package com.donornexa.admin.service;

import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.donation.repository.DonationRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.inventory.repository.BloodInventoryRepository;
import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.entity.User;
import com.donornexa.user.repository.UserRepository;

import com.donornexa.common.enums.Role;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service aggregating platform analytics and reports directly from database repositories.
 * Supports date-range filtering, monthly breakdowns, dynamic charts data, and CSV exports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final ProvinceRepository provinceRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> generateComprehensiveReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 1. User Metrics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findByAccountStatus(AccountStatus.ACTIVE).size();
        long suspendedUsers = userRepository.findByAccountStatus(AccountStatus.SUSPENDED).size();
        long pendingUsers = userRepository.findByAccountStatus(AccountStatus.PENDING_VERIFICATION).size();

        report.put("totalUsers", totalUsers);
        report.put("activeUsers", activeUsers);
        report.put("suspendedUsers", suspendedUsers);
        report.put("pendingUsers", pendingUsers);

        // Role Distribution
        Map<String, Long> roleDistribution = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            roleDistribution.put(r.name(), (long) userRepository.findByRole(r).size());
        }
        report.put("roleDistribution", roleDistribution);

        // 2. Donor Metrics
        long totalDonors = donorRepository.count();
        long verifiedDonors = donorRepository.countByVerified(Boolean.TRUE);
        long pendingDonors = donorRepository.countPendingVerification();
        long rejectedDonors = donorRepository.countRejectedVerification();

        report.put("totalDonors", totalDonors);
        report.put("verifiedDonors", verifiedDonors);
        report.put("pendingDonors", pendingDonors);
        report.put("rejectedDonors", rejectedDonors);

        // Blood Group Distribution among Donors
        Map<String, Long> donorBloodGroups = new LinkedHashMap<>();
        for (BloodGroup bg : BloodGroup.values()) {
            donorBloodGroups.put(bg.name().replace('_', ' '), donorRepository.countByBloodGroup(bg));
        }
        report.put("donorBloodGroups", donorBloodGroups);

        // Donor Availability Statistics
        Map<String, Long> donorAvailability = new LinkedHashMap<>();
        for (DonorAvailability da : DonorAvailability.values()) {
            donorAvailability.put(da.name(), donorRepository.countByAvailability(da));
        }
        report.put("donorAvailability", donorAvailability);

        // 3. Hospital Metrics
        long totalHospitals = hospitalRepository.count();
        long verifiedHospitals = hospitalRepository.countByVerified(Boolean.TRUE);
        long pendingHospitals = hospitalRepository.countPendingVerification();
        long rejectedHospitals = hospitalRepository.countRejectedVerification();

        report.put("totalHospitals", totalHospitals);
        report.put("verifiedHospitals", verifiedHospitals);
        report.put("pendingHospitals", pendingHospitals);
        report.put("rejectedHospitals", rejectedHospitals);

        // Hospital Activity Breakdown
        List<Hospital> hospitals = hospitalRepository.findAll();
        List<Map<String, Object>> hospitalActivity = new ArrayList<>();
        for (Hospital h : hospitals) {
            Map<String, Object> hMap = new LinkedHashMap<>();
            hMap.put("id", h.getId());
            hMap.put("name", h.getHospitalName());
            hMap.put("city", h.getCity() != null ? h.getCity().getName() : "N/A");
            hMap.put("province", h.getProvince() != null ? h.getProvince().getName() : "N/A");
            hMap.put("verified", h.getVerified());
            hMap.put("requestsCount", bloodRequestRepository.countByHospitalId(h.getId()));
            hMap.put("donationsCount", donationRepository.countByHospitalId(h.getId()));
            hospitalActivity.add(hMap);
        }
        report.put("hospitalActivity", hospitalActivity);

        // 4. Blood Request Metrics
        long totalRequests = bloodRequestRepository.count();
        long pendingRequests = bloodRequestRepository.countByStatus(RequestStatus.PENDING);
        long matchingRequests = bloodRequestRepository.countByStatus(RequestStatus.MATCHING);
        long fulfilledRequests = bloodRequestRepository.countByStatus(RequestStatus.FULFILLED);
        long cancelledRequests = bloodRequestRepository.countByStatus(RequestStatus.CANCELLED);

        report.put("totalRequests", totalRequests);
        report.put("pendingRequests", pendingRequests);
        report.put("matchingRequests", matchingRequests);
        report.put("fulfilledRequests", fulfilledRequests);
        report.put("cancelledRequests", cancelledRequests);

        // 5. Donation Metrics
        long totalDonations = donationRepository.count();
        long completedDonations = donationRepository.countByDonationStatus(DonationStatus.COMPLETED);
        long scheduledDonations = donationRepository.countByDonationStatus(DonationStatus.SCHEDULED);
        long cancelledDonations = donationRepository.countByDonationStatus(DonationStatus.CANCELLED);

        long totalUnitsCollected = donationRepository.findAll().stream()
                .filter(d -> d.getDonationStatus() == DonationStatus.COMPLETED)
                .mapToLong(Donation::getUnitsDonated)
                .sum();

        report.put("totalDonations", totalDonations);
        report.put("completedDonations", completedDonations);
        report.put("scheduledDonations", scheduledDonations);
        report.put("cancelledDonations", cancelledDonations);
        report.put("totalUnitsCollected", totalUnitsCollected);

        // 6. Monthly Statistics
        Map<String, Long> monthlyRequests = computeMonthlyRequests();
        Map<String, Long> monthlyDonations = computeMonthlyDonations();
        report.put("monthlyRequests", monthlyRequests);
        report.put("monthlyDonations", monthlyDonations);

        // 7. Inventory Metrics
        long totalInventoryUnits = bloodInventoryRepository.getTotalAvailableUnits();
        report.put("totalInventoryUnits", totalInventoryUnits);

        Map<String, Long> inventoryByGroup = new LinkedHashMap<>();
        for (BloodGroup bg : BloodGroup.values()) {
            inventoryByGroup.put(bg.name().replace('_', ' '), bloodInventoryRepository.getTotalAvailableUnitsByBloodGroup(bg));
        }
        report.put("inventoryByGroup", inventoryByGroup);

        return report;
    }

    /**
     * Build Chart.js payload for dynamic charts.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChartData() {
        Map<String, Object> comprehensive = generateComprehensiveReport();
        Map<String, Object> chartData = new LinkedHashMap<>();

        // 1. Blood group distribution
        @SuppressWarnings("unchecked")
        Map<String, Long> donorBg = (Map<String, Long>) comprehensive.get("donorBloodGroups");
        Map<String, Object> bgChart = new LinkedHashMap<>();
        bgChart.put("labels", new ArrayList<>(donorBg.keySet()));
        bgChart.put("data", new ArrayList<>(donorBg.values()));
        chartData.put("bloodGroupDistribution", bgChart);

        // 2. Request status distribution
        Map<String, Object> reqChart = new LinkedHashMap<>();
        reqChart.put("labels", List.of("Pending", "Matching", "Fulfilled", "Cancelled"));
        reqChart.put("data", List.of(
                comprehensive.get("pendingRequests"),
                comprehensive.get("matchingRequests"),
                comprehensive.get("fulfilledRequests"),
                comprehensive.get("cancelledRequests")
        ));
        chartData.put("requestStatusDistribution", reqChart);

        // 3. Monthly requests
        @SuppressWarnings("unchecked")
        Map<String, Long> mReq = (Map<String, Long>) comprehensive.get("monthlyRequests");
        Map<String, Object> mReqChart = new LinkedHashMap<>();
        mReqChart.put("labels", new ArrayList<>(mReq.keySet()));
        mReqChart.put("data", new ArrayList<>(mReq.values()));
        chartData.put("monthlyRequests", mReqChart);

        // 4. Monthly donations
        @SuppressWarnings("unchecked")
        Map<String, Long> mDon = (Map<String, Long>) comprehensive.get("monthlyDonations");
        Map<String, Object> mDonChart = new LinkedHashMap<>();
        mDonChart.put("labels", new ArrayList<>(mDon.keySet()));
        mDonChart.put("data", new ArrayList<>(mDon.values()));
        chartData.put("monthlyDonations", mDonChart);

        return chartData;
    }

    /**
     * CSV Export of Donors.
     */
    @Transactional(readOnly = true)
    public byte[] exportDonorsCsv(BloodGroup bloodGroup, DonorAvailability availability) {
        List<Donor> donors = donorRepository.findAllWithDetails();
        StringBuilder sb = new StringBuilder();
        sb.append("Donor ID,Full Name,Email,Phone,Blood Group,Availability,City,Province,Verified,Last Donation Date\n");
        for (Donor d : donors) {
            if (bloodGroup != null && d.getBloodGroup() != bloodGroup) continue;
            if (availability != null && d.getAvailability() != availability) continue;

            sb.append(d.getId()).append(",")
              .append(escapeCsv(d.getUser() != null ? d.getUser().getFullName() : "")).append(",")
              .append(escapeCsv(d.getUser() != null ? d.getUser().getEmail() : "")).append(",")
              .append(escapeCsv(d.getUser() != null ? d.getUser().getPhoneNumber() : "")).append(",")
              .append(escapeCsv(d.getBloodGroup() != null ? d.getBloodGroup().name() : "")).append(",")
              .append(escapeCsv(d.getAvailability() != null ? d.getAvailability().name() : "")).append(",")
              .append(escapeCsv(d.getCity() != null ? d.getCity().getName() : "")).append(",")
              .append(escapeCsv(d.getProvince() != null ? d.getProvince().getName() : "")).append(",")
              .append(Boolean.TRUE.equals(d.getVerified()) ? "YES" : "NO").append(",")
              .append(escapeCsv(d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : "None")).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CSV Export of Blood Requests.
     */
    @Transactional(readOnly = true)
    public byte[] exportRequestsCsv(RequestStatus status, BloodGroup bloodGroup, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<BloodRequest> requests = bloodRequestRepository.filterRequestsForReport(null, status, bloodGroup, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("Request ID,Hospital Name,Blood Group,Units Required,Units Fulfilled,Urgency,Status,City,Required By,Created At\n");
        for (BloodRequest br : requests) {
            sb.append(br.getId()).append(",")
              .append(escapeCsv(br.getHospital() != null ? br.getHospital().getHospitalName() : "")).append(",")
              .append(escapeCsv(br.getBloodGroup() != null ? br.getBloodGroup().name() : "")).append(",")
              .append(br.getUnitsRequired()).append(",")
              .append(br.getUnitsFulfilled()).append(",")
              .append(escapeCsv(br.getUrgency() != null ? br.getUrgency().name() : "")).append(",")
              .append(escapeCsv(br.getStatus() != null ? br.getStatus().name() : "")).append(",")
              .append(escapeCsv(br.getCity() != null ? br.getCity().getName() : "")).append(",")
              .append(escapeCsv(formatDateTime(br.getRequiredBy()))).append(",")
              .append(escapeCsv(formatDateTime(br.getCreatedAt()))).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CSV Export of Donations.
     */
    @Transactional(readOnly = true)
    public byte[] exportDonationsCsv(DonationStatus status, BloodGroup bloodGroup, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<Donation> donations = donationRepository.filterDonationsForReport(null, status, bloodGroup, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("Donation ID,Donor Name,Blood Group,Hospital,Units Donated,Donation Date,Status,Added To Inventory\n");
        for (Donation d : donations) {
            String donorName = d.getDonor() != null && d.getDonor().getUser() != null ? d.getDonor().getUser().getFullName() : "";
            String bg = d.getDonor() != null && d.getDonor().getBloodGroup() != null ? d.getDonor().getBloodGroup().name() : "";
            String hosp = d.getHospital() != null ? d.getHospital().getHospitalName() : "";

            sb.append(d.getId()).append(",")
              .append(escapeCsv(donorName)).append(",")
              .append(escapeCsv(bg)).append(",")
              .append(escapeCsv(hosp)).append(",")
              .append(d.getUnitsDonated()).append(",")
              .append(escapeCsv(formatDateTime(d.getDonationDate()))).append(",")
              .append(escapeCsv(d.getDonationStatus() != null ? d.getDonationStatus().name() : "")).append(",")
              .append(Boolean.TRUE.equals(d.getInventoryAdded()) ? "YES" : "NO").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CSV Export of Hospital Activity.
     */
    @Transactional(readOnly = true)
    public byte[] exportHospitalsCsv() {
        List<Hospital> list = hospitalRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Hospital ID,Hospital Name,Registration Number,City,Province,Verified,Total Requests,Total Donations,Created At\n");
        for (Hospital h : list) {
            sb.append(h.getId()).append(",")
              .append(escapeCsv(h.getHospitalName())).append(",")
              .append(escapeCsv(h.getRegistrationNumber())).append(",")
              .append(escapeCsv(h.getCity() != null ? h.getCity().getName() : "")).append(",")
              .append(escapeCsv(h.getProvince() != null ? h.getProvince().getName() : "")).append(",")
              .append(Boolean.TRUE.equals(h.getVerified()) ? "YES" : "NO").append(",")
              .append(bloodRequestRepository.countByHospitalId(h.getId())).append(",")
              .append(donationRepository.countByHospitalId(h.getId())).append(",")
              .append(escapeCsv(formatDateTime(h.getCreatedAt()))).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Long> computeMonthlyRequests() {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            result.put(ym.format(labelFormatter), 0L);
        }

        List<BloodRequest> requests = bloodRequestRepository.findAll();
        for (BloodRequest br : requests) {
            if (br.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(br.getCreatedAt());
                String key = ym.format(labelFormatter);
                if (result.containsKey(key)) {
                    result.put(key, result.get(key) + 1);
                }
            }
        }
        return result;
    }

    private Map<String, Long> computeMonthlyDonations() {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            result.put(ym.format(labelFormatter), 0L);
        }

        List<Donation> donations = donationRepository.findAll();
        for (Donation d : donations) {
            if (d.getDonationDate() != null) {
                YearMonth ym = YearMonth.from(d.getDonationDate());
                String key = ym.format(labelFormatter);
                if (result.containsKey(key)) {
                    result.put(key, result.get(key) + 1);
                }
            }
        }
        return result;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        // Prevent CSV Formula Injection
        if (!trimmed.isEmpty()) {
            char firstChar = trimmed.charAt(0);
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@') {
                value = "'" + value;
            }
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
