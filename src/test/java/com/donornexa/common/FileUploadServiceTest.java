package com.donornexa.common;

import com.donornexa.common.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CEP Audit — File Upload & Magic Bytes Security Tests")
class FileUploadServiceTest {

    @TempDir
    Path tempUploadDir;

    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(tempUploadDir.toString(), 2 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should accept valid JPEG image with FF D8 FF magic bytes")
    void shouldAcceptValidJpegImage() {
        byte[] validJpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        MockMultipartFile file = new MockMultipartFile("photo", "avatar.jpg", "image/jpeg", validJpegContent);

        String savedFilename = fileUploadService.uploadDonorPhoto(file);

        assertThat(savedFilename).isNotNull().startsWith("photo_").endsWith(".jpg");
    }

    @Test
    @DisplayName("Should accept valid PNG image with 89 50 4E 47 magic bytes")
    void shouldAcceptValidPngImage() {
        byte[] validPngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("photo", "profile.png", "image/png", validPngContent);

        String savedFilename = fileUploadService.uploadDonorPhoto(file);

        assertThat(savedFilename).isNotNull().startsWith("photo_").endsWith(".png");
    }

    @Test
    @DisplayName("Should reject photo with spoofed extension and invalid magic bytes")
    void shouldRejectSpoofedPhotoExtension() {
        byte[] maliciousContent = "<?php echo 'shell'; ?>".getBytes();
        MockMultipartFile spoofedFile = new MockMultipartFile("photo", "malicious.jpg", "image/jpeg", maliciousContent);

        assertThatThrownBy(() -> fileUploadService.uploadDonorPhoto(spoofedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File signature verification failed");
    }

    @Test
    @DisplayName("Should reject photo exceeding 2MB size limit")
    void shouldRejectPhotoExceedingSizeLimit() {
        byte[] largeContent = new byte[(2 * 1024 * 1024) + 10];
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;
        MockMultipartFile oversizedFile = new MockMultipartFile("photo", "large.jpg", "image/jpeg", largeContent);

        assertThatThrownBy(() -> fileUploadService.uploadDonorPhoto(oversizedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Photo file size exceeds maximum permitted limit");
    }

    @Test
    @DisplayName("Should accept valid PDF document with %PDF- magic bytes")
    void shouldAcceptValidPdfLicence() {
        byte[] validPdfContent = new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2D, 0x31, 0x2E, 0x37}; // %PDF-1.7
        MockMultipartFile file = new MockMultipartFile("licence", "hospital_license.pdf", "application/pdf", validPdfContent);

        String savedFilename = fileUploadService.uploadHospitalLicence(file);

        assertThat(savedFilename).isNotNull().startsWith("licence_").endsWith(".pdf");
    }

    @Test
    @DisplayName("Should reject licence file with invalid PDF signature")
    void shouldRejectInvalidPdfLicence() {
        byte[] fakePdf = "NOT_A_REAL_PDF_FILE".getBytes();
        MockMultipartFile file = new MockMultipartFile("licence", "hospital_license.pdf", "application/pdf", fakePdf);

        assertThatThrownBy(() -> fileUploadService.uploadHospitalLicence(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File signature verification failed");
    }
}
