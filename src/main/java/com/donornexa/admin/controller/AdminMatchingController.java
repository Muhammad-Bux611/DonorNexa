package com.donornexa.admin.controller;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.MatchStatus;
import com.donornexa.matching.dto.DonorMatchDTO;
import com.donornexa.matching.service.DonorMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for administrative monitoring of donor-to-request matches and responses.
 */
@Slf4j
@Controller
@RequestMapping("/admin/matches")
@RequiredArgsConstructor
public class AdminMatchingController {

    private final DonorMatchingService matchingService;

    @GetMapping
    public String monitorMatches(@RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "status", required = false) MatchStatus status,
                                 @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "15") int size,
                                 Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        Page<DonorMatchDTO> matchPage = matchingService.searchMatchesForAdmin(query, status, bloodGroup, pageable);

        model.addAttribute("matchPage", matchPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("statuses", MatchStatus.values());
        model.addAttribute("bloodGroups", BloodGroup.values());

        return "admin/matches";
    }
}
