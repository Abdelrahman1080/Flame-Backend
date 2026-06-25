package com.Flame.backend.services.reelsModeration;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles uploading local video files to Google Cloud Storage.
 *
 * Your videos are currently saved in the local "uploads" folder.
 * This service takes that local file and uploads it to GCS so that
 * the Video Intelligence API can read it for moderation scanning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GcsUploadService {

    private final Storage storage;

    @Value("${gcs.bucket.name}")
    private String bucketName;

    /**
     * Uploads a local video file to the GCS bucket.
     *
     * @param localFilePath  Absolute path to the video file on your server.
     *                       e.g. "/your-project/uploads/reel_123.mp4"
     * @param destinationFileName  The name to give the file inside the bucket.
     *                             e.g. "reels/reel_123.mp4"
     * @return The GCS URI of the uploaded file — e.g. "gs://flame-reels-bucket/reels/reel_123.mp4"
     *         This URI is what you pass to the Video Intelligence API.
     */
    public String uploadVideoToGcs(String localFilePath, String destinationFileName) throws IOException {
        Path path = Paths.get(localFilePath);
        byte[] videoBytes = Files.readAllBytes(path);

        BlobId blobId = BlobId.of(bucketName, destinationFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("video/mp4")
                .build();

        storage.create(blobInfo, videoBytes);

        String gcsUri = "gs://" + bucketName + "/" + destinationFileName;
        log.info("Uploaded video to GCS: {}", gcsUri);
        return gcsUri;
    }

    /**
     * Deletes a file from GCS after moderation is complete.
     * Optional — call this to save storage space once scanning is done.
     *
     * @param destinationFileName  The file name inside the bucket to delete.
     */
    public void deleteFromGcs(String destinationFileName) {
        BlobId blobId = BlobId.of(bucketName, destinationFileName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Deleted GCS file: {}", destinationFileName);
        } else {
            log.warn("Could not delete GCS file (may not exist): {}", destinationFileName);
        }
    }
}