package com.donornexa.hospital.dto;

import com.donornexa.inventory.entity.BloodStockEntry;
import com.donornexa.common.enums.BloodGroup;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class HospitalInventorySummaryDTO {

    private long totalAvailableUnits;
    private long totalReservedUnits;
    private long nearExpiryUnits;
    private long expiredUnits;
    private Map<BloodGroup, Long> unitsByGroup;
    private List<BloodStockEntry> activeBatches;
    private List<BloodStockEntry> nearExpiryBatches;
    private List<BloodStockEntry> expiredBatches;

    public long getUnits(BloodGroup bg) {
        if (unitsByGroup == null || bg == null) {
            return 0L;
        }
        return unitsByGroup.getOrDefault(bg, 0L);
    }
}
