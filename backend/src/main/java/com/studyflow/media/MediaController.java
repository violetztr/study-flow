package com.studyflow.media;

import com.studyflow.common.ApiResponse;
import com.studyflow.media.dto.MediaUploadCompleteResponse;
import com.studyflow.media.dto.MediaUploadPrepareRequest;
import com.studyflow.media.dto.MediaUploadPrepareResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/uploads/presign")
    public ApiResponse<MediaUploadPrepareResponse> prepareImageUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MediaUploadPrepareRequest request
    ) {
        return ApiResponse.success(mediaService.prepareImageUpload(principal.userId(), request));
    }

    @PostMapping("/uploads/{mediaFileId}/complete")
    public ApiResponse<MediaUploadCompleteResponse> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        return ApiResponse.success(mediaService.completeUpload(principal.userId(), mediaFileId));
    }
}
