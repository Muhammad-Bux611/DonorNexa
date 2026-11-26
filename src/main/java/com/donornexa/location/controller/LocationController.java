package com.donornexa.location.controller;

import com.donornexa.location.dto.CityDto;
import com.donornexa.location.dto.ProvinceDto;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing public location endpoints for dynamic dropdowns.
 */
@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final CityRepository cityRepository;

    /**
     * Retrieve all active cities across Pakistan as JSON.
     *
     * GET /api/cities
     */
    @GetMapping(value = "/api/cities", params = "!provinceId")
    public ResponseEntity<List<CityDto>> getAllCities() {
        return ResponseEntity.ok(cityRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CityDto(c.getId(), c.getName()))
                .toList());
    }

    /**
     * Retrieve all administrative areas (provinces/territories).
     *
     * GET /api/locations/provinces
     */
    @GetMapping("/api/locations/provinces")
    public ResponseEntity<List<ProvinceDto>> getAllProvinces() {
        return ResponseEntity.ok(locationService.getAllProvinces());
    }

    /**
     * Retrieve cities belonging to the specified province.
     *
     * GET /api/locations/provinces/{provinceId}/cities
     */
    @GetMapping("/api/locations/provinces/{provinceId}/cities")
    public ResponseEntity<List<CityDto>> getCitiesByProvince(@PathVariable Long provinceId) {
        return locationService.getCitiesByProvinceId(provinceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
