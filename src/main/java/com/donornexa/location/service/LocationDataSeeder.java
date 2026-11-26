package com.donornexa.location.service;

import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically seeds the database with official Pakistan administrative areas
 * and major cities upon application startup.
 *
 * <p>Idempotent: Only inserts missing records; existing records are preserved.
 * Safe to run repeatedly on every application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationDataSeeder implements CommandLineRunner {

    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;

    private static final Map<String, List<String>> SEED_DATA = new LinkedHashMap<>();

    static {
        SEED_DATA.put("Punjab", List.of(
                "Lahore", "Faisalabad", "Rawalpindi", "Multan", "Gujranwala",
                "Sialkot", "Bahawalpur", "Sargodha", "Sheikhupura", "Rahim Yar Khan"
        ));

        SEED_DATA.put("Sindh", List.of(
                "Karachi", "Hyderabad", "Sukkur", "Larkana", "Mirpur Khas",
                "Nawabshah", "Jacobabad", "Shikarpur", "Thatta", "Khairpur"
        ));

        SEED_DATA.put("Khyber Pakhtunkhwa", List.of(
                "Peshawar", "Mardan", "Abbottabad", "Swat", "Kohat",
                "Bannu", "Dera Ismail Khan", "Nowshera", "Mansehra", "Swabi"
        ));

        SEED_DATA.put("Balochistan", List.of(
                "Quetta", "Gwadar", "Turbat", "Khuzdar", "Chaman",
                "Sibi", "Loralai", "Zhob"
        ));

        SEED_DATA.put("Islamabad Capital Territory", List.of(
                "Islamabad"
        ));

        SEED_DATA.put("Gilgit-Baltistan", List.of(
                "Gilgit", "Skardu", "Hunza", "Gahkuch", "Astore"
        ));

        SEED_DATA.put("Azad Jammu and Kashmir", List.of(
                "Muzaffarabad", "Mirpur", "Kotli", "Rawalakot", "Bagh", "Bhimber"
        ));
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedLocations();
    }

    /**
     * Seeds missing provinces and cities.
     * Safe to run repeatedly without creating duplicates.
     */
    @Transactional
    public void seedLocations() {
        log.info("Checking and seeding Pakistan location data...");
        int provincesAdded = 0;
        int citiesAdded = 0;

        for (Map.Entry<String, List<String>> entry : SEED_DATA.entrySet()) {
            String provinceName = entry.getKey();
            List<String> cityNames = entry.getValue();

            Province province = provinceRepository.findByName(provinceName)
                    .orElseGet(() -> {
                        Province newProvince = new Province();
                        newProvince.setName(provinceName);
                        return provinceRepository.save(newProvince);
                    });

            if (province.getId() == null) {
                province = provinceRepository.save(province);
            }

            for (String cityName : cityNames) {
                if (!cityRepository.existsByNameAndProvinceId(cityName, province.getId())) {
                    City city = new City();
                    city.setName(cityName);
                    city.setProvince(province);
                    cityRepository.save(city);
                    citiesAdded++;
                }
            }
        }

        log.info("Location seeding check completed. Added {} new cities across {} administrative areas.",
                citiesAdded, SEED_DATA.size());
    }
}
