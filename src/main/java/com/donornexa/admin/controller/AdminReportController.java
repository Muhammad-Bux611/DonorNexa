package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminReportService;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller providing comprehensive analytical reporting, dynamic Chart.js data,
 * and CSV exports for platform administrators.
 */
@Slf4j
@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public String viewReports(Model model) {
        Map<String, Object> reportData = adminReportService.generateComprehensiveReport();
        model.addAllAttributes(reportData);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("requestStatuses", RequestStatus.values());
        model.addAttribute("donationStatuses", DonationStatus.values());
        model.addAttribute("availabilities", DonorAvailability.values());
        return "admin/reports";
    }

    /**
     * REST endpoint supplying real-time statistics to client-side Chart.js.
     */
    @GetMapping("/chart-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChartData() {
        return ResponseEntity.ok(adminReportService.getChartData());
    }

    /**
     * CSV export endpoint for administrator reports.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(value = "reportType", defaultValue = "donors") String reportType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
            @RequestParam(value = "availability", required = false) DonorAvailability availability,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] csvData;
        String filename;

        switch (reportType.toLowerCase()) {
            case "requests":
                RequestStatus rStatus = null;
                if (status != null && !status.isBlank()) {
                    try { rStatus = RequestStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                csvData = adminReportService.exportRequestsCsv(rStatus, bloodGroup, startDate, endDate);
                filename = "admin_blood_requests_report.csv";
                break;
            case "donations":
                DonationStatus dStatus = null;
                if (status != null && !status.isBlank()) {
                    try { dStatus = DonationStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }
                csvData = adminReportService.exportDonationsCsv(dStatus, bloodGroup, startDate, endDate);
                filename = "admin_donations_report.csv";
                break;
            case "hospitals":
                csvData = adminReportService.exportHospitalsCsv();
                filename = "admin_hospitals_report.csv";
                break;
            case "donors":
            default:
                csvData = adminReportService.exportDonorsCsv(bloodGroup, availability);
                filename = "admin_donors_report.csv";
                break;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
