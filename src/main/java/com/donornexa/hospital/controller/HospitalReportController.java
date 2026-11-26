package com.donornexa.hospital.controller;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.hospital.dto.HospitalReportSummaryDTO;
import com.donornexa.hospital.service.HospitalReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller providing hospital-specific reporting, analytical breakdowns,
 * dynamic chart JSON endpoints, and CSV report downloads.
 * Strictly restricted to authenticated users with ROLE_HOSPITAL.
 */
@Slf4j
@Controller
@RequestMapping("/hospital/reports")
@PreAuthorize("hasRole('HOSPITAL')")
@RequiredArgsConstructor
public class HospitalReportController {

    private final HospitalReportService hospitalReportService;

    @GetMapping
    public String viewReports(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "tab", defaultValue = "summary") String activeTab,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        String email = userDetails.getUsername();
        HospitalReportSummaryDTO summary = hospitalReportService.getReportSummary(email);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));

        model.addAttribute("summary", summary);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("userEmail", email);

        if ("requests".equalsIgnoreCase(activeTab)) {
            RequestStatus reqStatus = null;
            if (status != null && !status.isBlank()) {
                try { reqStatus = RequestStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            Page<BloodRequest> requestsPage = hospitalReportService.getRequestsReport(email, reqStatus, bloodGroup, startDate, endDate, pageable);
            model.addAttribute("requestsPage", requestsPage);
            model.addAttribute("selectedStatus", reqStatus);
            model.addAttribute("requestStatuses", RequestStatus.values());
        } else if ("matches".equalsIgnoreCase(activeTab)) {
            MatchStatus matchStatus = null;
            if (status != null && !status.isBlank()) {
                try { matchStatus = MatchStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            Page<RequestMatch> matchesPage = hospitalReportService.getMatchesReport(email, matchStatus, bloodGroup, startDate, endDate, pageable);
            model.addAttribute("matchesPage", matchesPage);
            model.addAttribute("selectedStatus", matchStatus);
            model.addAttribute("matchStatuses", MatchStatus.values());
        } else if ("donations".equalsIgnoreCase(activeTab)) {
            DonationStatus donStatus = null;
            if (status != null && !status.isBlank()) {
                try { donStatus = DonationStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            Page<Donation> donationsPage = hospitalReportService.getDonationsReport(email, donStatus, bloodGroup, startDate, endDate, pageable);
            model.addAttribute("donationsPage", donationsPage);
            model.addAttribute("selectedStatus", donStatus);
            model.addAttribute("donationStatuses", DonationStatus.values());
        }

        return "hospital/reports";
    }

    /**
     * REST endpoint supplying real-time statistics to client-side Chart.js.
     */
    @GetMapping("/chart-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChartData(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Map<String, Object> chartData = hospitalReportService.getChartData(email);
        return ResponseEntity.ok(chartData);
    }

    /**
     * Secure CSV export strictly scoped to the authenticated hospital.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "reportType", defaultValue = "requests") String reportType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = userDetails.getUsername();
        byte[] csvData;
        String filename;

        switch (reportType.toLowerCase()) {
            case "matches":
                MatchStatus mStatus = null;
                if (status != null && !status.isBlank()) {
                    try { mStatus = MatchStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                csvData = hospitalReportService.exportMatchesCsv(email, mStatus, bloodGroup, startDate, endDate);
                filename = "hospital_matches_report.csv";
                break;
            case "donations":
                DonationStatus dStatus = null;
                if (status != null && !status.isBlank()) {
                    try { dStatus = DonationStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                csvData = hospitalReportService.exportDonationsCsv(email, dStatus, bloodGroup, startDate, endDate);
                filename = "hospital_donations_report.csv";
                break;
            case "requests":
            default:
                RequestStatus rStatus = null;
                if (status != null && !status.isBlank()) {
                    try { rStatus = RequestStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                csvData = hospitalReportService.exportRequestsCsv(email, rStatus, bloodGroup, startDate, endDate);
                filename = "hospital_requests_report.csv";
                break;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
