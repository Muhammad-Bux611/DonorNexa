package com.donornexa.inventory.repository;


import com.donornexa.inventory.entity.BloodInventory;
import com.donornexa.common.enums.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link BloodInventory} entities.
 */
@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    /**
     * Find the inventory record for a specific hospital and blood group.
     * Returns empty when no inventory record exists yet for that combination.
     */
    Optional<BloodInventory> findByHospitalIdAndBloodGroup(Long hospitalId, BloodGroup bloodGroup);

    /** Find all inventory records for a specific hospital. */
    List<BloodInventory> findByHospitalId(Long hospitalId);

    /** Find all inventory records for a specific hospital ordered by blood group. */
    List<BloodInventory> findByHospitalIdOrderByBloodGroupAsc(Long hospitalId);

    /** Calculate total available blood units for a specific hospital. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(bi.availableUnits), 0) FROM BloodInventory bi WHERE bi.hospital.id = :hospitalId")
    long getTotalAvailableUnitsByHospital(@org.springframework.data.repository.query.Param("hospitalId") Long hospitalId);

    /** Calculate available blood units for a specific hospital and blood group. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(bi.availableUnits), 0) FROM BloodInventory bi WHERE bi.hospital.id = :hospitalId AND bi.bloodGroup = :bloodGroup")
    long getTotalAvailableUnitsByHospitalAndBloodGroup(
            @org.springframework.data.repository.query.Param("hospitalId") Long hospitalId,
            @org.springframework.data.repository.query.Param("bloodGroup") BloodGroup bloodGroup);

    /** Find inventory records across all hospitals for a specific blood group. */
    List<BloodInventory> findByBloodGroup(BloodGroup bloodGroup);

    /** Check whether an inventory record already exists for the hospital/blood-group pair. */
    boolean existsByHospitalIdAndBloodGroup(Long hospitalId, BloodGroup bloodGroup);

    /** Find all inventory records with eager-loaded hospital. */
    @org.springframework.data.jpa.repository.Query("SELECT bi FROM BloodInventory bi JOIN FETCH bi.hospital h ORDER BY bi.bloodGroup ASC, h.hospitalName ASC")
    List<BloodInventory> findAllWithDetails();

    /** Calculate total available blood units across all hospitals. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(bi.availableUnits), 0) FROM BloodInventory bi")
    long getTotalAvailableUnits();

    /** Calculate total available units for a specific blood group. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(bi.availableUnits), 0) FROM BloodInventory bi WHERE bi.bloodGroup = :bloodGroup")
    long getTotalAvailableUnitsByBloodGroup(@org.springframework.data.repository.query.Param("bloodGroup") BloodGroup bloodGroup);
}
