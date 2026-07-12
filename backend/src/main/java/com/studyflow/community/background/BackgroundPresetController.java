package com.studyflow.community.background;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.background.dto.BackgroundPresetRequest;
import com.studyflow.community.background.dto.BackgroundPresetResponse;
import com.studyflow.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BackgroundPresetController {
    private final BackgroundPresetService backgroundPresetService;

    public BackgroundPresetController(BackgroundPresetService backgroundPresetService) {
        this.backgroundPresetService = backgroundPresetService;
    }

    @GetMapping("/background-presets")
    public ApiResponse<List<BackgroundPresetResponse>> listPresets(
            @RequestParam(defaultValue = "HOME") String placement
    ) {
        return ApiResponse.success(backgroundPresetService.listPresets(placement));
    }

    @PostMapping("/admin/background-presets")
    public ApiResponse<BackgroundPresetResponse> createPreset(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BackgroundPresetRequest request
    ) {
        return ApiResponse.success(backgroundPresetService.createPreset(principal.userId(), request));
    }
}
