package com.donornexa;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class DonorNexaApplication {

    @PostConstruct
    public void init() {
        // Set JVM default time zone to Asia/Karachi as mandated by CEP
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"));
    }

    public static void main(String[] args) {
        SpringApplication.run(DonorNexaApplication.class, args);
    }
}
