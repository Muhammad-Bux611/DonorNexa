package com.donornexa.location.repository;

import com.donornexa.location.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link City} entities.
 */
@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    /** Find all cities across all provinces ordered alphabetically by name. */
    List<City> findAllByOrderByNameAsc();

    /** Find all cities that belong to the given province. */
    List<City> findByProvinceId(Long provinceId);

    /** Find all cities that belong to the given province, ordered alphabetically by name. */
    List<City> findByProvinceIdOrderByNameAsc(Long provinceId);

    /**
     * Check whether a city with the given name already exists
     * within the specified province (enforces composite uniqueness at query level).
     */
    boolean existsByNameAndProvinceId(String name, Long provinceId);

    /** Find all cities whose name matches the given value across all provinces. */
    List<City> findByName(String name);
}
