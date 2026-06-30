package com.Flame.backend.services.reelsModeration;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsFileUploadService {

    private final Storage storage;

    @Value("${gcs.bucket.name}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > MAX_FILE_SIZE)
            throw new RuntimeException("File too large. Max size is 10MB");

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.startsWith("image/"))
            throw new RuntimeException("Only image files are allowed");

        return upload(file.getBytes(), contentType, folder, file.getOriginalFilename());
    }

    public String uploadVideo(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType() == null ? "video/mp4" : file.getContentType();

        return upload(file.getBytes(), contentType, folder, file.getOriginalFilename());
    }

    public void deleteFile(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return;

        try {
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";
            if (publicUrl.startsWith(prefix)) {
                String objectName = publicUrl.substring(prefix.length());
                BlobId blobId = BlobId.of(bucketName, objectName);
                boolean deleted = storage.delete(blobId);
                if (deleted) log.info("Deleted GCS file: {}", objectName);
                else log.warn("Could not delete GCS file: {}", objectName);
            }
        } catch (Exception e) {
            log.warn("Failed to delete GCS file: {}", publicUrl);
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private String upload(byte[] bytes, String contentType, String folder, String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        String objectName = folder + "/" + UUID.randomUUID() + extension;

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        // ✅ Works with uniform bucket-level access — no per-object ACL needed
        // Make sure your bucket has "allUsers" granted Storage Object Viewer in IAM
        storage.create(blobInfo, bytes);

        String publicUrl = "https://storage.googleapis.com/" + bucketName + "/" + objectName;
        log.info("Uploaded file to GCS: {}", publicUrl);
        return publicUrl;
    }
}