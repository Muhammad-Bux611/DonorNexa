package com.donornexa.location.dto;

/**
 * Data transfer object representing an administrative province or territory.
 *
 * @param id   Unique database identifier
 * @param name Official administrative area name
 */
public record ProvinceDto(Long id, String name) {}
