package com.donornexa.donor.service;

import com.donornexa.donor.dto.DonorEligibilityResult;
import com.donornexa.donor.entity.Donor;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.DonorAvailability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service evaluating blood donor medical and operational eligibility
 * against the university CEP approved criteria and international/Pakistan standards.
 *
 * <p>Eligibility Rules:
 * <ol>
 *   <li><b>Age:</b> 18 to 60 years inclusive.</li>
 *   <li><b>Weight:</b> Minimum 50.0 kg.</li>
 *   <li><b>Donation Interval:</b> Minimum 90 days cooldown between whole-blood donations.</li>
 *   <li><b>Operational Availability:</b> Status must be {@link DonorAvailability#AVAILABLE}.</li>
 *   <li><b>Account Status:</b> Account must not be SUSPENDED or INACTIVE.</li>
 * </ol>
 */
@Slf4j
@Service
public class DonorEligibilityService {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 60;
    public static final BigDecimal MIN_WEIGHT = BigDecimal.valueOf(50.0);
    public static final long DONATION_COOLDOWN_DAYS = 90L;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.donornexa.admin.repository.SettingRepository settingRepository;

    public long getCooldownDays() {
        if (settingRepository != null) {
            return settingRepository.getIntSetting("donor.cooldown.days", (int) DONATION_COOLDOWN_DAYS);
        }
        return DONATION_COOLDOWN_DAYS;
    }

    /**
     * Evaluates a donor's current eligibility.
     *
     * @param donor The donor entity to evaluate
     * @return Detailed {@link DonorEligibilityResult}
     */
    public DonorEligibilityResult evaluateEligibility(Donor donor) {
        if (donor == null) {
            throw new IllegalArgumentException("Donor cannot be null for eligibility evaluation");
        }

        long activeCooldownDays = getCooldownDays();
        LocalDate today = LocalDate.now();
        List<String> criteriaMessages = new ArrayList<>();

        // 1. Age Evaluation
        int age = Period.between(donor.getDateOfBirth(), today).getYears();
        boolean ageEligible = age >= MIN_AGE && age <= MAX_AGE;
        if (age < MIN_AGE) {
            criteriaMessages.add(String.format("Age ineligible: You are %d years old. Donors must be at least %d years old.", age, MIN_AGE));
        } else if (age > MAX_AGE) {
            criteriaMessages.add(String.format("Age ineligible: You are %d years old. The upper age limit for routine blood donation is %d years.", age, MAX_AGE));
        } else {
            criteriaMessages.add(String.format("Age eligible: %d years old (acceptable range %dâ€“%d years).", age, MIN_AGE, MAX_AGE));
        }

        // 2. Weight Evaluation
        boolean weightEligible;
        if (donor.getWeight() == null) {
            weightEligible = false;
            criteriaMessages.add(String.format("Weight pending: Body weight is unrecorded. Minimum %.1f kg is required to donate blood.", MIN_WEIGHT.doubleValue()));
        } else if (donor.getWeight().compareTo(MIN_WEIGHT) < 0) {
            weightEligible = false;
            criteriaMessages.add(String.format("Weight ineligible: Current weight is %.1f kg. Minimum required weight is %.1f kg.",
                    donor.getWeight().doubleValue(), MIN_WEIGHT.doubleValue()));
        } else {
            weightEligible = true;
            criteriaMessages.add(String.format("Weight eligible: %.1f kg (minimum %.1f kg satisfied).",
                    donor.getWeight().doubleValue(), MIN_WEIGHT.doubleValue()));
        }

        // 3. Donation Interval / Cooldown Evaluation
        boolean cooldownEligible;
        Long daysSinceLastDonation = null;
        Long daysRemainingCooldown = 0L;
        LocalDate nextEligibleDate = null;

        if (donor.getLastDonationDate() != null) {
            daysSinceLastDonation = ChronoUnit.DAYS.between(donor.getLastDonationDate(), today);
            if (daysSinceLastDonation < activeCooldownDays) {
                cooldownEligible = false;
                daysRemainingCooldown = activeCooldownDays - daysSinceLastDonation;
                nextEligibleDate = donor.getLastDonationDate().plusDays(activeCooldownDays);
                criteriaMessages.add(String.format("Cooldown active: Last donation was %d days ago (%s). %d-day interval required. Next eligible on %s (%d days remaining).",
                        daysSinceLastDonation, donor.getLastDonationDate(), activeCooldownDays, nextEligibleDate, daysRemainingCooldown));
            } else {
                cooldownEligible = true;
                criteriaMessages.add(String.format("Cooldown satisfied: %d days elapsed since last donation (%s).",
                        daysSinceLastDonation, donor.getLastDonationDate()));
            }
        } else {
            cooldownEligible = true;
            criteriaMessages.add("First-time donor: No previous donation cooldown active.");
        }

        // 4. Operational Availability
        boolean availabilityEligible = (donor.getAvailability() == DonorAvailability.AVAILABLE);
        if (!availabilityEligible) {
            criteriaMessages.add(String.format("Availability status: Currently set to '%s'. Set status to Available when ready to donate.",
                    donor.getAvailability().name().replace('_', ' ')));
        } else {
            criteriaMessages.add("Availability status: Available to receive matching donation requests.");
        }

        // 5. Account Status
        User user = donor.getUser();
        boolean accountEligible = user != null &&
                user.getAccountStatus() != AccountStatus.SUSPENDED &&
                user.getAccountStatus() != AccountStatus.INACTIVE &&
                Boolean.TRUE.equals(user.getEnabled());
        if (!accountEligible) {
            criteriaMessages.add("Account status: Account is currently restricted or suspended.");
        }

        // Overall decision
        boolean overallEligible = ageEligible && weightEligible && cooldownEligible && availabilityEligible && accountEligible;

        String summaryMessage;
        String badgeClass;
        if (overallEligible) {
            summaryMessage = "You meet all health and interval requirements to donate blood.";
            badgeClass = "bg-success";
        } else if (!availabilityEligible && ageEligible && weightEligible && cooldownEligible && accountEligible) {
            summaryMessage = "You meet health requirements, but your status is currently set to Unavailable.";
            badgeClass = "bg-warning text-dark";
        } else if (!cooldownEligible) {
            summaryMessage = String.format("Donation cooldown active: You will be eligible to donate again on %s.", nextEligibleDate);
            badgeClass = "bg-info text-dark";
        } else {
            summaryMessage = "You do not currently meet all criteria required for blood donation.";
            badgeClass = "bg-danger";
        }

        return DonorEligibilityResult.builder()
                .eligible(overallEligible)
                .age(age)
                .ageEligible(ageEligible)
                .weightEligible(weightEligible)
                .cooldownEligible(cooldownEligible)
                .availabilityEligible(availabilityEligible)
                .accountEligible(accountEligible)
                .daysSinceLastDonation(daysSinceLastDonation)
                .daysRemainingCooldown(daysRemainingCooldown)
                .nextEligibleDate(nextEligibleDate)
                .criteriaMessages(criteriaMessages)
                .summaryMessage(summaryMessage)
                .statusBadgeClass(badgeClass)
                .build();
    }
}
