package com.donornexa.security;

import com.donornexa.bloodrequest.dto.CreateBloodRequestDTO;
import com.donornexa.common.util.PhoneMaskingUtil;
import com.donornexa.user.entity.*;
import com.donornexa.donor.entity.*;
import com.donornexa.hospital.entity.*;
import com.donornexa.bloodrequest.entity.*;
import com.donornexa.matching.entity.*;
import com.donornexa.donation.entity.*;
import com.donornexa.inventory.entity.*;
import com.donornexa.notification.entity.*;
import com.donornexa.location.entity.*;
import com.donornexa.messaging.entity.*;
import com.donornexa.audit.entity.*;
import com.donornexa.admin.entity.*;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.hospital.service.HospitalBloodRequestService;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.user.repository.*;
import com.donornexa.donor.repository.*;
import com.donornexa.hospital.repository.*;
import com.donornexa.bloodrequest.repository.*;
import com.donornexa.matching.repository.*;
import com.donornexa.donation.repository.*;
import com.donornexa.inventory.repository.*;
import com.donornexa.notification.repository.*;
import com.donornexa.location.repository.*;
import com.donornexa.messaging.repository.*;
import com.donornexa.audit.repository.*;
import com.donornexa.admin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CEP Audit â€” Anti-Misuse Quota & Security Hardening Tests")
class AntiMisuseAndSecurityTest {

    @Mock private BloodRequestRepository bloodRequestRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private CityRepository cityRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    private HospitalBloodRequestService hospitalBloodRequestService;

    @BeforeEach
    void setUp() {
        hospitalBloodRequestService = new HospitalBloodRequestService(
                bloodRequestRepository,
                hospitalRepository,
                provinceRepository,
                cityRepository,
                auditLogRepository,
                userRepository,
                notificationService
        );
    }

    @Test
    @DisplayName("Anti-misuse quota: Hospital cannot create > 3 simultaneous open CRITICAL requests")
    void shouldRejectFourthCriticalBloodRequest() {
        Hospital hospital = new Hospital();
        hospital.setId(10L);
        hospital.setHospitalName("Aga Khan Hospital");
        hospital.setVerified(true);

        when(hospitalRepository.findByUserEmailWithDetails("akuh@donornexa.com"))
                .thenReturn(Optional.of(hospital));

        // Mock that hospital already has 3 active CRITICAL requests
        when(bloodRequestRepository.countByHospitalIdAndUrgencyAndStatusIn(
                eq(10L), eq(RequestUrgency.CRITICAL), any()))
                .thenReturn(3L);

        CreateBloodRequestDTO dto = new CreateBloodRequestDTO();
        dto.setBloodGroup(BloodGroup.O_NEGATIVE);
        dto.setUnitsRequired(2);
        dto.setUrgency(RequestUrgency.CRITICAL);
        dto.setProvinceId(1L);
        dto.setCityId(1L);

        assertThatThrownBy(() -> hospitalBloodRequestService.createRequest("akuh@donornexa.com", dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Anti-misuse quota exceeded");

        verify(bloodRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hospital cancellation penalizes trust score by 5 points and writes audit log")
    void shouldPenalizeTrustScoreOnCancellation() {
        User user = new User();
        user.setEmail("hospital@donornexa.com");

        Hospital hospital = new Hospital();
        hospital.setId(20L);
        hospital.setUser(user);
        hospital.setHospitalName("Indus Hospital");
        hospital.setTrustScore(95);

        BloodRequest request = new BloodRequest();
        request.setId(201L);
        request.setHospital(hospital);
        request.setStatus(RequestStatus.MATCHING);

        when(hospitalRepository.findByUserEmailWithDetails("hospital@donornexa.com"))
                .thenReturn(Optional.of(hospital));
        when(bloodRequestRepository.findByIdWithDetails(201L))
                .thenReturn(Optional.of(request));
        when(bloodRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BloodRequest cancelled = hospitalBloodRequestService.cancelRequest(201L, "hospital@donornexa.com");

        assertThat(cancelled.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(hospital.getTrustScore()).isEqualTo(90); // 95 - 5 = 90
        verify(hospitalRepository).save(hospital);
        verify(auditLogRepository, atLeast(2)).save(any()); // STATUS_CHANGE and TRUST_SCORE_UPDATE
    }

    @Test
    @DisplayName("Contact privacy masking formats phone numbers securely")
    void shouldMaskContactPhoneCorrectly() {
        String raw = "+92-300-2101001";
        String masked = PhoneMaskingUtil.maskPhoneNumber(raw);

        assertThat(masked).isEqualTo("+92-30-***1001");
        assertThat(masked).doesNotContain("210"); // Middle digits masked
    }
}
