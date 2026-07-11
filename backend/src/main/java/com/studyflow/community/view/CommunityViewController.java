package com.studyflow.community.view;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.view.dto.CommunityViewReportRequest;
import com.studyflow.community.view.dto.CommunityViewReportResponse;
import com.studyflow.community.view.dto.CommunityWatchHistoryResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityViewController {
    private final CommunityViewService communityViewService;

    public CommunityViewController(CommunityViewService communityViewService) {
        this.communityViewService = communityViewService;
    }

    @PostMapping("/posts/{postId}/views")
    public ApiResponse<CommunityViewReportResponse> reportPlayback(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityViewReportRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = principal == null ? null : principal.userId();
        String viewerKey = userId == null ? guestViewerKey(httpRequest) : "user:" + userId;
        return ApiResponse.success(communityViewService.reportPlayback(userId, viewerKey, postId, request));
    }

    @GetMapping("/views/history/my")
    public ApiResponse<List<CommunityWatchHistoryResponse>> listMyHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(communityViewService.listMyHistory(principal.userId()));
    }

    private String guestViewerKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        String userAgent = request.getHeader("User-Agent");
        return "guest:" + sha256(ip + "|" + (userAgent == null ? "" : userAgent));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
