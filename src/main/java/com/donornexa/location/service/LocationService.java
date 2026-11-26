package com.donornexa.location.service;

import com.donornexa.location.dto.CityDto;
import com.donornexa.location.dto.ProvinceDto;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service providing database-driven administrative area (province) and city data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;

    /**
     * Retrieve all administrative areas ordered alphabetically by name.
     *
     * @return List of province DTOs
     */
    public List<ProvinceDto> getAllProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream()
                .map(p -> new ProvinceDto(p.getId(), p.getName()))
                .toList();
    }

    /**
     * Retrieve all cities belonging to the specified province, ordered alphabetically by name.
     *
     * @param provinceId Database identifier of the province
     * @return Optional containing the list of city DTOs, or Optional.empty() if the province does not exist
     */
    public Optional<List<CityDto>> getCitiesByProvinceId(Long provinceId) {
        if (!provinceRepository.existsById(provinceId)) {
            log.warn("Lookup requested for non-existent province ID: {}", provinceId);
            return Optional.empty();
        }

        List<CityDto> cities = cityRepository.findByProvinceIdOrderByNameAsc(provinceId).stream()
                .map(c -> new CityDto(c.getId(), c.getName()))
                .toList();
        return Optional.of(cities);
    }
}
