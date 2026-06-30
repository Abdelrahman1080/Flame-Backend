package com.Flame.backend.controllers.reels;

import com.Flame.backend.DTO.report.AdminReportReviewRequest;
import com.Flame.backend.DTO.report.ReelReportResponse;
import com.Flame.backend.DTO.report.ReportReelRequest;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.reels.ReelReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelReportController {

    private final ReelReportService reelReportService;

    // ── User ─────────────────────────────────────────────────────────────────

    // POST /api/reels/{reelId}/report
    @PostMapping("/{reelId}/report")
    public ResponseEntity<ReelReportResponse> reportReel(
            Authentication authentication,
            @PathVariable Long reelId,
            @RequestBody ReportReelRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reelReportService.reportReel(currentUser.getId(), reelId, request));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    // GET /api/reels/reports/pending
    @GetMapping("/reports/pending")
    public ResponseEntity<List<ReelReportResponse>> getPendingReports(Authentication authentication) {
        User admin=(User) authentication.getPrincipal();
        if (!admin.getRole().name().equals("ADMIN")) {
            //add messege
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build() ;
        }
        return ResponseEntity.ok(reelReportService.getPendingReports());
    }

    // GET /api/reels/reports/all
    @GetMapping("/reports/all")
    public ResponseEntity<List<ReelReportResponse>> getAllReports(Authentication authentication) {
        User admin=(User) authentication.getPrincipal();
        if (!admin.getRole().name().equals("ADMIN")) {
            //add messege
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build() ;
        }
        return ResponseEntity.ok(reelReportService.getAllReports());
    }

    // GET /api/reels/{reelId}/reports
    @GetMapping("/{reelId}/reports")
    public ResponseEntity<List<ReelReportResponse>> getReportsByReel(@PathVariable Long reelId,Authentication authentication) {
        User admin=(User) authentication.getPrincipal();
        if (!admin.getRole().name().equals("ADMIN")) {
            //add messege
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build() ;
        }
        return ResponseEntity.ok(reelReportService.getReportsByReel(reelId));
    }

    // PATCH /api/reels/reports/{reportId}/review
    @PatchMapping("/reports/{reportId}/review")
    public ResponseEntity<ReelReportResponse> reviewReport(
            @PathVariable Long reportId,
            @RequestBody AdminReportReviewRequest request,
            Authentication authentication
    ) {

        User admin=(User) authentication.getPrincipal();
        if (!admin.getRole().name().equals("ADMIN")) {
            //add messege
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build() ;
        }
        return ResponseEntity.ok(reelReportService.reviewReport(reportId, request));
    }
}
