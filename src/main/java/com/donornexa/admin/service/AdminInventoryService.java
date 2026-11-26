package com.donornexa.admin.service;

import com.donornexa.inventory.entity.BloodInventory;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.inventory.repository.BloodInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing centralized blood inventory administration and group metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInventoryService {

    private final BloodInventoryRepository bloodInventoryRepository;

    @Transactional(readOnly = true)
    public List<BloodInventory> getAllInventory() {
        return bloodInventoryRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public long getTotalAvailableUnits() {
        return bloodInventoryRepository.getTotalAvailableUnits();
    }

    @Transactional(readOnly = true)
    public Map<BloodGroup, Long> getUnitsByBloodGroupMap() {
        Map<BloodGroup, Long> map = new LinkedHashMap<>();
        for (BloodGroup bg : BloodGroup.values()) {
            map.put(bg, bloodInventoryRepository.getTotalAvailableUnitsByBloodGroup(bg));
        }
        return map;
    }
}
