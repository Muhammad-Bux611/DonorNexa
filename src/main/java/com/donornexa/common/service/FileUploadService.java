package com.donornexa.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service for securely uploading and validating donor profile photos and hospital medical licenses.
 * Enforces file size limitations, extension whitelisting, and deep binary magic byte verification.
 */
@Slf4j
@Service
public class FileUploadService {

    private final Path uploadRoot;
    private final long maxPhotoSize;
    private final long maxLicenceSize;

    // Magic Bytes signatures
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC  = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
    private static final byte[] PDF_MAGIC  = new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2D}; // %PDF-

    public FileUploadService(
            @Value("${app.upload.dir:${donornexa.upload.dir:uploads}}") String uploadDir,
            @Value("${donornexa.upload.max-file-size:2097152}") long maxPhotoSize) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxPhotoSize = maxPhotoSize; // 2 MB
        this.maxLicenceSize = 5L * 1024 * 1024; // 5 MB
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(uploadRoot.resolve("photos"));
            Files.createDirectories(uploadRoot.resolve("licences"));
        } catch (IOException ex) {
            log.error("Failed to initialize upload storage directories: {}", ex.getMessage());
        }
    }

    /**
     * Securely uploads and validates a voluntary donor profile photo.
     * Allowed formats: JPG, JPEG, PNG. Max size: 2MB.
     *
     * @param file Incoming multipart file
     * @return Stored filename
     */
    public String uploadDonorPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > maxPhotoSize) {
            throw new IllegalArgumentException("Photo file size exceeds maximum permitted limit of 2MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file: missing file extension.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
            throw new IllegalArgumentException("Invalid image format. Only JPG and PNG are accepted.");
        }

        // Validate Magic Bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 3) {
                throw new IllegalArgumentException("Corrupted or empty image file.");
            }

            boolean isJpeg = header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
            boolean isPng = read >= 8 && Arrays.equals(header, PNG_MAGIC);

            if (!isJpeg && !isPng) {
                throw new IllegalArgumentException("File signature verification failed: File magic bytes do not match valid JPEG or PNG images.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect file magic bytes: " + ex.getMessage(), ex);
        }

        // Generate safe randomized storage filename
        String safeFilename = "photo_" + UUID.randomUUID() + "." + (extension.equals("jpeg") ? "jpg" : extension);
        Path targetLocation = uploadRoot.resolve("photos").resolve(safeFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Donor photo securely stored: {}", safeFilename);
            return safeFilename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded photo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Securely uploads and validates an accredited hospital medical licence.
     * Allowed format: PDF (%PDF- signature). Max size: 5MB.
     *
     * @param file Incoming multipart file
     * @return Stored filename
     */
    public String uploadHospitalLicence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > maxLicenceSize) {
            throw new IllegalArgumentException("Licence document file size exceeds maximum permitted limit of 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Invalid file: missing file extension.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("pdf")) {
            throw new IllegalArgumentException("Invalid document format. Only official PDF documents are accepted.");
        }

        // Validate Magic Bytes (%PDF-)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[5];
            int read = is.read(header);
            if (read < 5 || !Arrays.equals(header, PDF_MAGIC)) {
                throw new IllegalArgumentException("File signature verification failed: File magic bytes do not match a valid PDF document (%PDF-).");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect document magic bytes: " + ex.getMessage(), ex);
        }

        String safeFilename = "licence_" + UUID.randomUUID() + ".pdf";
        Path targetLocation = uploadRoot.resolve("licences").resolve(safeFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Hospital licence document securely stored: {}", safeFilename);
            return safeFilename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded licence: " + ex.getMessage(), ex);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
