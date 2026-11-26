package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminAuditLogService;
import com.donornexa.audit.entity.AuditLog;
import com.donornexa.common.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for inspecting immutable system audit logs for security and compliance.
 */
@Controller
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @GetMapping
    public String viewAuditLogs(@RequestParam(value = "query", required = false) String query,
                                @RequestParam(value = "action", required = false) AuditAction action,
                                @RequestParam(value = "entityName", required = false) String entityName,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "15") int size,
                                Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("createdAt").descending());

        Page<AuditLog> logPage = adminAuditLogService.searchAuditLogs(query, action, entityName, pageable);

        model.addAttribute("logPage", logPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedEntity", entityName != null ? entityName : "");
        model.addAttribute("actions", AuditAction.values());

        return "admin/audit-logs";
    }
}
