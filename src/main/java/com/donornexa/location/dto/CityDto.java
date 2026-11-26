package com.donornexa.location.dto;

/**
 * Data transfer object representing a city.
 *
 * @param id   Unique database identifier
 * @param name City name
 */
public record CityDto(Long id, String name) {}
