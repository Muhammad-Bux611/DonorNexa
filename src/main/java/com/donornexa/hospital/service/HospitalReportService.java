package com.donornexa.hospital.service;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.hospital.dto.HospitalReportSummaryDTO;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.donation.repository.DonationRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.matching.repository.RequestMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Service providing hospital-level reporting, dynamic charts data,
 * and secure CSV report exports isolated strictly to the calling hospital facility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalReportService {

    private final HospitalRepository hospitalRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final RequestMatchRepository requestMatchRepository;
    private final DonationRepository donationRepository;

    @Transactional(readOnly = true)
    public Hospital getHospitalByEmail(String email) {
        return hospitalRepository.findByUserEmailWithDetails(email.toLowerCase().trim())
                .orElseThrow(() -> new HospitalNotFoundException("Hospital profile not found for user: " + email));
    }

    /**
     * Compute comprehensive reporting metrics for the hospital.
     */
    @Transactional(readOnly = true)
    public HospitalReportSummaryDTO getReportSummary(String email) {
        Hospital hospital = getHospitalByEmail(email);
        Long hospitalId = hospital.getId();

        // Blood requests counts
        long totalRequests = bloodRequestRepository.countByHospitalId(hospitalId);
        long pendingRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospitalId, RequestStatus.PENDING);
        long matchingRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospitalId, RequestStatus.MATCHING);
        long fulfilledRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospitalId, RequestStatus.FULFILLED);
        long cancelledRequests = bloodRequestRepository.countByHospitalIdAndStatus(hospitalId, RequestStatus.CANCELLED);

        // Matches counts
        long totalMatches = requestMatchRepository.countByBloodRequestHospitalId(hospitalId);
        long acceptedMatches = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospitalId, MatchStatus.ACCEPTED);
        long declinedMatches = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospitalId, MatchStatus.REJECTED);
        long pendingMatches = requestMatchRepository.countByBloodRequestHospitalIdAndMatchStatus(hospitalId, MatchStatus.PENDING);

        // Donations counts
        long totalDonations = donationRepository.countByHospitalId(hospitalId);
        long completedDonations = donationRepository.countByHospitalIdAndDonationStatus(hospitalId, DonationStatus.COMPLETED);
        long scheduledDonations = donationRepository.countByHospitalIdAndDonationStatus(hospitalId, DonationStatus.SCHEDULED);

        List<Donation> allHospitalDonations = donationRepository.findByHospitalId(hospitalId);
        long totalUnitsCollected = allHospitalDonations.stream()
                .filter(d -> d.getDonationStatus() == DonationStatus.COMPLETED)
                .mapToLong(Donation::getUnitsDonated)
                .sum();

        // Monthly trends (past 6 months)
        Map<String, Long> monthlyRequests = computeMonthlyRequests(hospitalId);
        Map<String, Long> monthlyDonations = computeMonthlyDonations(hospitalId);

        return HospitalReportSummaryDTO.builder()
                .hospitalName(hospital.getHospitalName())
                .registrationNumber(hospital.getRegistrationNumber())
                .totalRequests(totalRequests)
                .pendingRequests(pendingRequests)
                .matchingRequests(matchingRequests)
                .fulfilledRequests(fulfilledRequests)
                .cancelledRequests(cancelledRequests)
                .totalMatches(totalMatches)
                .acceptedMatches(acceptedMatches)
                .declinedMatches(declinedMatches)
                .pendingMatches(pendingMatches)
                .totalDonations(totalDonations)
                .completedDonations(completedDonations)
                .scheduledDonations(scheduledDonations)
                .totalUnitsCollected(totalUnitsCollected)
                .monthlyRequests(monthlyRequests)
                .monthlyDonations(monthlyDonations)
                .build();
    }

    /**
     * Filter hospital blood requests.
     */
    @Transactional(readOnly = true)
    public Page<BloodRequest> getRequestsReport(String email, RequestStatus status, BloodGroup bloodGroup,
                                               LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return bloodRequestRepository.filterHospitalRequests(hospital.getId(), status, bloodGroup, start, end, pageable);
    }

    /**
     * Filter hospital donor matches.
     */
    @Transactional(readOnly = true)
    public Page<RequestMatch> getMatchesReport(String email, MatchStatus status, BloodGroup bloodGroup,
                                              LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return requestMatchRepository.filterHospitalMatches(hospital.getId(), status, bloodGroup, start, end, pageable);
    }

    /**
     * Filter hospital donations.
     */
    @Transactional(readOnly = true)
    public Page<Donation> getDonationsReport(String email, DonationStatus status, BloodGroup bloodGroup,
                                            LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return donationRepository.filterHospitalDonations(hospital.getId(), status, bloodGroup, start, end, pageable);
    }

    /**
     * Build Chart.js payload for dynamic client-side rendering.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChartData(String email) {
        HospitalReportSummaryDTO summary = getReportSummary(email);
        Map<String, Object> data = new HashMap<>();

        // 1. Request status distribution
        Map<String, Object> requestStatusChart = new LinkedHashMap<>();
        requestStatusChart.put("labels", List.of("Pending", "Matching", "Fulfilled", "Cancelled"));
        requestStatusChart.put("data", List.of(
                summary.getPendingRequests(),
                summary.getMatchingRequests(),
                summary.getFulfilledRequests(),
                summary.getCancelledRequests()
        ));
        data.put("requestStatusDistribution", requestStatusChart);

        // 2. Monthly requests trend
        Map<String, Object> monthlyReqChart = new LinkedHashMap<>();
        monthlyReqChart.put("labels", new ArrayList<>(summary.getMonthlyRequests().keySet()));
        monthlyReqChart.put("data", new ArrayList<>(summary.getMonthlyRequests().values()));
        data.put("monthlyRequests", monthlyReqChart);

        // 3. Monthly donations trend
        Map<String, Object> monthlyDonChart = new LinkedHashMap<>();
        monthlyDonChart.put("labels", new ArrayList<>(summary.getMonthlyDonations().keySet()));
        monthlyDonChart.put("data", new ArrayList<>(summary.getMonthlyDonations().values()));
        data.put("monthlyDonations", monthlyDonChart);

        return data;
    }

    /**
     * Generate CSV export of hospital blood requests.
     */
    @Transactional(readOnly = true)
    public byte[] exportRequestsCsv(String email, RequestStatus status, BloodGroup bloodGroup,
                                    LocalDate startDate, LocalDate endDate) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<BloodRequest> list = bloodRequestRepository.filterRequestsForReport(hospital.getId(), status, bloodGroup, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("Request ID,Blood Group,Units Required,Units Fulfilled,Urgency,Status,Required By,Created At,Hospital Notes\n");
        for (BloodRequest br : list) {
            sb.append(escapeCsv(String.valueOf(br.getId()))).append(",")
              .append(escapeCsv(br.getBloodGroup().name())).append(",")
              .append(br.getUnitsRequired()).append(",")
              .append(br.getUnitsFulfilled()).append(",")
              .append(escapeCsv(br.getUrgency().name())).append(",")
              .append(escapeCsv(br.getStatus().name())).append(",")
              .append(escapeCsv(formatDateTime(br.getRequiredBy()))).append(",")
              .append(escapeCsv(formatDateTime(br.getCreatedAt()))).append(",")
              .append(escapeCsv(br.getHospitalNotes())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate CSV export of matched donor coordination records (sensitive CNIC excluded).
     */
    @Transactional(readOnly = true)
    public byte[] exportMatchesCsv(String email, MatchStatus status, BloodGroup bloodGroup,
                                   LocalDate startDate, LocalDate endDate) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        Page<RequestMatch> page = requestMatchRepository.filterHospitalMatches(hospital.getId(), status, bloodGroup, start, end, Pageable.unpaged());

        StringBuilder sb = new StringBuilder();
        sb.append("Match ID,Request ID,Blood Group,Donor Masked Identity,City,Status,Compatibility Score,Matched At,Response Notes\n");
        for (RequestMatch rm : page.getContent()) {
            String donorMask = rm.getDonor() != null && rm.getDonor().getUser() != null
                    ? maskDonorName(rm.getDonor().getUser().getFullName()) : "Anonymous";
            String city = rm.getDonor() != null && rm.getDonor().getCity() != null ? rm.getDonor().getCity().getName() : "N/A";
            sb.append(rm.getId()).append(",")
              .append(rm.getBloodRequest().getId()).append(",")
              .append(escapeCsv(rm.getBloodRequest().getBloodGroup().name())).append(",")
              .append(escapeCsv(donorMask)).append(",")
              .append(escapeCsv(city)).append(",")
              .append(escapeCsv(rm.getMatchStatus().name())).append(",")
              .append(rm.getMatchScore()).append(",")
              .append(escapeCsv(formatDateTime(rm.getMatchedAt()))).append(",")
              .append(escapeCsv(rm.getDonorNotes())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate CSV export of recorded donations.
     */
    @Transactional(readOnly = true)
    public byte[] exportDonationsCsv(String email, DonationStatus status, BloodGroup bloodGroup,
                                     LocalDate startDate, LocalDate endDate) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<Donation> list = donationRepository.filterDonationsForReport(hospital.getId(), status, bloodGroup, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("Donation ID,Donor Name,Blood Group,Units Donated,Donation Date,Status,Added To Inventory,Notes\n");
        for (Donation d : list) {
            String donorName = d.getDonor() != null && d.getDonor().getUser() != null
                    ? d.getDonor().getUser().getFullName() : "Anonymous";
            String bg = d.getDonor() != null ? d.getDonor().getBloodGroup().name() : "N/A";
            sb.append(d.getId()).append(",")
              .append(escapeCsv(donorName)).append(",")
              .append(escapeCsv(bg)).append(",")
              .append(d.getUnitsDonated()).append(",")
              .append(escapeCsv(formatDateTime(d.getDonationDate()))).append(",")
              .append(escapeCsv(d.getDonationStatus().name())).append(",")
              .append(Boolean.TRUE.equals(d.getInventoryAdded()) ? "YES" : "NO").append(",")
              .append(escapeCsv(d.getNotes())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Long> computeMonthlyRequests(Long hospitalId) {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();

        // Initialize last 6 months in chronological order
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            result.put(ym.format(labelFormatter), 0L);
        }

        List<BloodRequest> requests = bloodRequestRepository.findByHospitalId(hospitalId);
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

    private Map<String, Long> computeMonthlyDonations(Long hospitalId) {
        Map<String, Long> result = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            result.put(ym.format(labelFormatter), 0L);
        }

        List<Donation> donations = donationRepository.findByHospitalId(hospitalId);
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

    private String maskDonorName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Donor";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].charAt(0) + "***";
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
