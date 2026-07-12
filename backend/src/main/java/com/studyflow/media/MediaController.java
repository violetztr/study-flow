package com.studyflow.media;

import com.studyflow.common.ApiResponse;
import com.studyflow.media.dto.MediaUploadCompleteResponse;
import com.studyflow.media.dto.MediaUploadPrepareRequest;
import com.studyflow.media.dto.MediaUploadPrepareResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/media/uploads/presign")
    public ApiResponse<MediaUploadPrepareResponse> prepareUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MediaUploadPrepareRequest request
    ) {
        return ApiResponse.success(mediaService.prepareUpload(principal.userId(), request));
    }

    @PostMapping("/media/uploads/{mediaFileId}/complete")
    public ApiResponse<MediaUploadCompleteResponse> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        return ApiResponse.success(mediaService.completeUpload(principal.userId(), mediaFileId));
    }

    @GetMapping("/admin/media/pending")
    public ApiResponse<List<MediaUploadCompleteResponse>> listPendingReviewMedia(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(mediaService.listPendingReviewMedia(principal.userId()));
    }

    @PostMapping("/admin/media/{mediaFileId}/approve")
    public ApiResponse<MediaUploadCompleteResponse> approveVideo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        return ApiResponse.success(mediaService.approveVideo(principal.userId(), mediaFileId));
    }

    @PostMapping("/admin/media/{mediaFileId}/reject")
    public ApiResponse<MediaUploadCompleteResponse> rejectVideo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        return ApiResponse.success(mediaService.rejectVideo(principal.userId(), mediaFileId));
    }

    @PostMapping("/admin/media/{mediaFileId}/transcode/retry")
    public ApiResponse<MediaUploadCompleteResponse> retryVideoTranscode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        return ApiResponse.success(mediaService.retryVideoTranscode(principal.userId(), mediaFileId));
    }

    @GetMapping(value = "/media/videos/{mediaFileId}/hls/master.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<String> hlsMasterPlaylist(@PathVariable Long mediaFileId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(mediaService.buildHlsMasterPlaylist(mediaFileId));
    }

    @GetMapping(value = "/media/videos/{mediaFileId}/hls/{quality}/index.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<String> hlsVariantPlaylist(
            @PathVariable Long mediaFileId,
            @PathVariable String quality
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(mediaService.buildHlsVariantPlaylist(mediaFileId, quality));
    }

    @GetMapping("/media/videos/{mediaFileId}/hls/{quality}/segments/{segmentIndex}.ts")
    public ResponseEntity<Void> hlsSegment(
            @PathVariable Long mediaFileId,
            @PathVariable String quality,
            @PathVariable Integer segmentIndex
    ) {
        return ResponseEntity.status(302)
                .location(URI.create(mediaService.presignHlsSegmentUrl(mediaFileId, quality, segmentIndex)))
                .build();
    }
}
