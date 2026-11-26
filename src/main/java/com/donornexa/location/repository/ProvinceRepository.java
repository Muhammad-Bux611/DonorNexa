package com.donornexa.location.repository;

import com.donornexa.location.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Province} entities.
 */
@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    /** Find a province by its unique name. */
    Optional<Province> findByName(String name);

    /** Check whether a province with the given name exists. */
    boolean existsByName(String name);

    /** Find all provinces ordered by name ascending. */
    List<Province> findAllByOrderByNameAsc();
}
