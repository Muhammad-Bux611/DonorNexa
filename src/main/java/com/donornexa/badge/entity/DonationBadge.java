package com.donornexa.badge.entity;


import lombok.Getter;

/**
 * Gamification recognition milestone badges for voluntary blood donors.
 */
@Getter
public enum DonationBadge {
    BRONZE_LIFESAVER(1, "Bronze Lifesaver", "Completed 1st life-saving blood donation", "fa-medal", "#cd7f32"),
    SILVER_GUARDIAN(5, "Silver Guardian", "Completed 5 life-saving blood donations", "fa-shield-halved", "#c0c0c0"),
    GOLD_HERO(10, "Gold Hero", "Completed 10+ life-saving blood donations", "fa-crown", "#ffd700");

    private final int requiredDonations;
    private final String displayName;
    private final String description;
    private final String iconClass;
    private final String colorHex;

    DonationBadge(int requiredDonations, String displayName, String description, String iconClass, String colorHex) {
        this.requiredDonations = requiredDonations;
        this.displayName = displayName;
        this.description = description;
        this.iconClass = iconClass;
        this.colorHex = colorHex;
    }
}
