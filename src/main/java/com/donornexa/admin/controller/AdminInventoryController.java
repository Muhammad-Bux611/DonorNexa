package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminInventoryService;
import com.donornexa.inventory.entity.BloodInventory;
import com.donornexa.common.enums.BloodGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * Controller for centralized administrative oversight of blood inventory across hospitals.
 */
@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    @GetMapping
    public String viewInventory(Model model) {
        List<BloodInventory> inventoryList = adminInventoryService.getAllInventory();
        long totalUnits = adminInventoryService.getTotalAvailableUnits();
        Map<BloodGroup, Long> unitsByGroup = adminInventoryService.getUnitsByBloodGroupMap();

        model.addAttribute("inventoryList", inventoryList);
        model.addAttribute("totalUnits", totalUnits);
        model.addAttribute("unitsByGroup", unitsByGroup);
        model.addAttribute("bloodGroups", BloodGroup.values());

        return "admin/inventory";
    }
}
