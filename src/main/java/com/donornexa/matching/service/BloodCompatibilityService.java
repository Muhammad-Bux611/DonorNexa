package com.donornexa.matching.service;

import com.donornexa.common.enums.BloodGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Service encapsulating red blood cell ABO and Rh(D) compatibility rules
 * based on clinical transfusion guidelines and university CEP approved specifications.
 *
 * <p>Compatibility Table for Whole Blood / Red Blood Cell Transfusion:
 * <ul>
 *   <li><b>O-</b>: Universal red cell donor. Can donate to O-, O+, A-, A+, B-, B+, AB-, AB+.</li>
 *   <li><b>O+</b>: Can donate to O+, A+, B+, AB+.</li>
 *   <li><b>A-</b>: Can donate to A-, A+, AB-, AB+.</li>
 *   <li><b>A+</b>: Can donate to A+, AB+.</li>
 *   <li><b>B-</b>: Can donate to B-, B+, AB-, AB+.</li>
 *   <li><b>B+</b>: Can donate to B+, AB+.</li>
 *   <li><b>AB-</b>: Can donate to AB-, AB+.</li>
 *   <li><b>AB+</b>: Universal red cell recipient. Can donate only to AB+.</li>
 * </ul>
 */
@Slf4j
@Service
public class BloodCompatibilityService {

    /** Maps a recipient's blood group to the set of compatible donor blood groups. */
    private static final Map<BloodGroup, Set<BloodGroup>> RECIPIENT_TO_DONORS = new EnumMap<>(BloodGroup.class);

    /** Maps a donor's blood group to the set of compatible recipient blood groups. */
    private static final Map<BloodGroup, Set<BloodGroup>> DONOR_TO_RECIPIENTS = new EnumMap<>(BloodGroup.class);

    static {
        // Recipient can receive from:
        RECIPIENT_TO_DONORS.put(BloodGroup.O_NEGATIVE, EnumSet.of(BloodGroup.O_NEGATIVE));
        RECIPIENT_TO_DONORS.put(BloodGroup.O_POSITIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE));

        RECIPIENT_TO_DONORS.put(BloodGroup.A_NEGATIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE));
        RECIPIENT_TO_DONORS.put(BloodGroup.A_POSITIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE));

        RECIPIENT_TO_DONORS.put(BloodGroup.B_NEGATIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE));
        RECIPIENT_TO_DONORS.put(BloodGroup.B_POSITIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE));

        RECIPIENT_TO_DONORS.put(BloodGroup.AB_NEGATIVE, EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE,
                BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE));
        RECIPIENT_TO_DONORS.put(BloodGroup.AB_POSITIVE, EnumSet.allOf(BloodGroup.class));

        // Invert to build donor to recipients map
        for (BloodGroup bg : BloodGroup.values()) {
            DONOR_TO_RECIPIENTS.put(bg, EnumSet.noneOf(BloodGroup.class));
        }
        for (Map.Entry<BloodGroup, Set<BloodGroup>> entry : RECIPIENT_TO_DONORS.entrySet()) {
            BloodGroup recipient = entry.getKey();
            for (BloodGroup donor : entry.getValue()) {
                DONOR_TO_RECIPIENTS.get(donor).add(recipient);
            }
        }
    }

    /**
     * Checks if a donor blood group is red blood cell compatible with a recipient blood group.
     *
     * @param donorGroup Blood group of the prospective donor
     * @param recipientGroup Blood group required by the recipient / blood request
     * @return true if clinically compatible; false otherwise
     */
    public boolean isCompatible(BloodGroup donorGroup, BloodGroup recipientGroup) {
        if (donorGroup == null || recipientGroup == null) {
            return false;
        }
        Set<BloodGroup> compatibleDonors = RECIPIENT_TO_DONORS.get(recipientGroup);
        return compatibleDonors != null && compatibleDonors.contains(donorGroup);
    }

    /**
     * Checks if the donor blood group is an exact identity match to the recipient blood group.
     */
    public boolean isExactMatch(BloodGroup donorGroup, BloodGroup recipientGroup) {
        return donorGroup != null && donorGroup == recipientGroup;
    }

    /**
     * Retrieves the set of all donor blood groups that can safely donate to the specified recipient.
     *
     * @param recipientGroup Required recipient blood group
     * @return Unmodifiable set of compatible donor blood groups
     */
    public Set<BloodGroup> getCompatibleDonorGroups(BloodGroup recipientGroup) {
        if (recipientGroup == null) {
            return Collections.emptySet();
        }
        Set<BloodGroup> groups = RECIPIENT_TO_DONORS.get(recipientGroup);
        return groups != null ? Collections.unmodifiableSet(groups) : Collections.emptySet();
    }

    /**
     * Retrieves the set of all recipient blood groups that the specified donor can safely donate to.
     *
     * @param donorGroup Donor blood group
     * @return Unmodifiable set of compatible recipient blood groups
     */
    public Set<BloodGroup> getCompatibleRecipientGroups(BloodGroup donorGroup) {
        if (donorGroup == null) {
            return Collections.emptySet();
        }
        Set<BloodGroup> groups = DONOR_TO_RECIPIENTS.get(donorGroup);
        return groups != null ? Collections.unmodifiableSet(groups) : Collections.emptySet();
    }

    /**
     * Produces a human-readable description of compatibility for audit logging and display.
     */
    public String getCompatibilityDescription(BloodGroup donorGroup, BloodGroup recipientGroup) {
        if (!isCompatible(donorGroup, recipientGroup)) {
            return String.format("Incompatible: %s donor red cells cannot be transfused to %s recipient.",
                    donorGroup, recipientGroup);
        }
        if (isExactMatch(donorGroup, recipientGroup)) {
            return String.format("Exact match: %s donor matches %s requirement directly.",
                    donorGroup, recipientGroup);
        }
        if (donorGroup == BloodGroup.O_NEGATIVE) {
            return String.format("Universal red blood cell compatibility: O- donor can be received by %s.",
                    recipientGroup);
        }
        return String.format("Compatible cross-match: %s donor is compatible for %s recipient.",
                donorGroup, recipientGroup);
    }
}
