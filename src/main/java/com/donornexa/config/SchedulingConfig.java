package com.donornexa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring task scheduling for automated background operations
 * such as the emergency blood request auto-expiry scheduler.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
