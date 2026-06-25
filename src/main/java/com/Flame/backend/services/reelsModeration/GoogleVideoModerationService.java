package com.Flame.backend.services.reelsModeration;

import com.Flame.backend.DTO.moderation.AiModerationResponse;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.videointelligence.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleVideoModerationService {

    private final GoogleCredentials googleCredentials;

    // Flag if explicit content likelihood is POSSIBLE or higher
    private static final Likelihood EXPLICIT_THRESHOLD = Likelihood.POSSIBLE;

    // Minimum label confidence to include (0.0 – 1.0)
    private static final float LABEL_CONFIDENCE_THRESHOLD = 0.1f;

    // Labels that indicate a policy violation — caught via LABEL_DETECTION
    private static final List<String> VIOLATION_LABELS = List.of(
            "VIOLENCE", "BLOOD", "WEAPON", "GUN", "KNIFE", "FIGHT",
            "FIGHTING", "DRUG", "DRUGS", "DRUG_USE", "SELF_HARM",
            "HATE_SPEECH", "NUDITY", "PORNOGRAPHY","MMA",

            "INJURY", "WOUND", "BLEEDING", "BODILY_INJURY",
            "PHYSICAL_TRAUMA", "GORE"
    );

    // ── Main entry point ──────────────────────────────────────────────────────

    public AiModerationResponse moderateVideo(String gcsUri) {
        log.info("Starting Video Intelligence scan for: {}", gcsUri);

        try (VideoIntelligenceServiceClient client = buildClient()) {

            // Only two valid v1 features for moderation
            AnnotateVideoRequest request = AnnotateVideoRequest.newBuilder()
                    .setInputUri(gcsUri)
                    .addFeatures(Feature.EXPLICIT_CONTENT_DETECTION)
                    .addFeatures(Feature.LABEL_DETECTION)
                    .build();

            log.info("Submitting Video Intelligence job...");
            OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> future =
                    client.annotateVideoAsync(request);

            AnnotateVideoResponse response = future.get(5, TimeUnit.MINUTES);
            VideoAnnotationResults results = response.getAnnotationResults(0);

            log.info("Scan complete for: {}", gcsUri);
            return parseResults(results);

        } catch (Exception ex) {
            log.error("Video Intelligence API failed for {}: {}", gcsUri, ex.getMessage());
            return AiModerationResponse.builder()
                    .flagged(false)
                    .confidenceScore(0.0)
                    .reason("Video scan unavailable — defaulting to approved.")
                    .violationCategories("NONE")
                    .contentLabels("UNKNOWN")
                    .build();
        }
    }

    // ── Result parsing ────────────────────────────────────────────────────────

    private AiModerationResponse parseResults(VideoAnnotationResults results) {
        List<String> violations = new ArrayList<>();
        List<String> contentLabels = new ArrayList<>();
        double maxConfidence = 0.0;

        // ── 1. Explicit content detection (nudity / sexual) ───────────────────
        ExplicitContentAnnotation explicitAnnotation = results.getExplicitAnnotation();
        for (ExplicitContentFrame frame : explicitAnnotation.getFramesList()) {
            Likelihood likelihood = frame.getPornographyLikelihood();
            if (likelihood.getNumber() >= EXPLICIT_THRESHOLD.getNumber()) {
                violations.add("NUDITY");
                maxConfidence = Math.max(maxConfidence, likelihoodToScore(likelihood));
                break; // one flagged frame is enough
            }
        }

        // ── 2. Label detection — both content categorization AND violence ──────
        for (LabelAnnotation label : results.getSegmentLabelAnnotationsList()) {
            for (LabelSegment segment : label.getSegmentsList()) {
                if (segment.getConfidence() >= LABEL_CONFIDENCE_THRESHOLD) {
                    String labelName = label.getEntity().getDescription()
                            .toUpperCase().replace(" ", "_");

                    // Check if this label is a policy violation
                    if (VIOLATION_LABELS.contains(labelName)
                            && !violations.contains(labelName)) {
                        violations.add(labelName);
                        maxConfidence = Math.max(maxConfidence, segment.getConfidence());
                    } else if (!contentLabels.contains(labelName)) {
                        // Otherwise treat as a content category label
                        contentLabels.add(labelName);
                    }
                    break;
                }
            }
        }

        // Also check shot-level labels
        for (LabelAnnotation label : results.getShotLabelAnnotationsList()) {
            for (LabelSegment segment : label.getSegmentsList()) {
                if (segment.getConfidence() >= LABEL_CONFIDENCE_THRESHOLD) {
                    String labelName = label.getEntity().getDescription()
                            .toUpperCase().replace(" ", "_");

                    if (VIOLATION_LABELS.contains(labelName)
                            && !violations.contains(labelName)) {
                        violations.add(labelName);
                        maxConfidence = Math.max(maxConfidence, segment.getConfidence());
                    } else if (!contentLabels.contains(labelName)) {
                        contentLabels.add(labelName);
                    }
                    break;
                }
            }
        }

        // ── Build response ────────────────────────────────────────────────────
        boolean flagged = !violations.isEmpty();

        return AiModerationResponse.builder()
                .flagged(flagged)
                .confidenceScore(maxConfidence)
                .reason(flagged
                        ? "Video contains flagged content: "
                          + String.join(", ", violations).toLowerCase().replace("_", " ") + "."
                        : "No violation detected.")
                .violationCategories(flagged ? String.join(", ", violations) : "NONE")
                .contentLabels(contentLabels.isEmpty() ? "GENERAL"
                        : String.join(", ", contentLabels))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double likelihoodToScore(Likelihood likelihood) {
        return switch (likelihood) {
            case VERY_LIKELY   -> 0.95;
            case LIKELY        -> 0.80;
            case POSSIBLE      -> 0.60;
            case UNLIKELY      -> 0.30;
            case VERY_UNLIKELY -> 0.05;
            default            -> 0.0;
        };
    }

    private VideoIntelligenceServiceClient buildClient() throws Exception {
        VideoIntelligenceServiceSettings settings = VideoIntelligenceServiceSettings
                .newBuilder()
                .setCredentialsProvider(() -> googleCredentials)
                .build();
        return VideoIntelligenceServiceClient.create(settings);
    }
}