package com.studyflow.community.background;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.background.dto.BackgroundPresetRequest;
import com.studyflow.community.background.dto.BackgroundPresetResponse;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BackgroundPresetService {
    private static final String PLACEMENT_PROFILE = "PROFILE";
    private static final String MEDIA_TYPE_IMAGE = "IMAGE";
    private static final String MEDIA_TYPE_VIDEO = "VIDEO";
    private static final String RURU_ADMIN_USERNAME = "ruru";

    private final BackgroundPresetMapper backgroundPresetMapper;
    private final UserMapper userMapper;

    public BackgroundPresetService(BackgroundPresetMapper backgroundPresetMapper, UserMapper userMapper) {
        this.backgroundPresetMapper = backgroundPresetMapper;
        this.userMapper = userMapper;
    }

    public List<BackgroundPresetResponse> listPresets(String placement) {
        String normalizedPlacement = normalizePlacement(placement);
        return backgroundPresetMapper.selectList(new LambdaQueryWrapper<BackgroundPreset>()
                        .eq(BackgroundPreset::getPlacement, normalizedPlacement)
                        .orderByAsc(BackgroundPreset::getSortOrder)
                        .orderByAsc(BackgroundPreset::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BackgroundPresetResponse createPreset(Long adminUserId, BackgroundPresetRequest request) {
        requireRuruAdmin(adminUserId);

        LocalDateTime now = LocalDateTime.now();
        BackgroundPreset preset = new BackgroundPreset();
        preset.setPlacement(normalizePlacement(request.placement()));
        preset.setName(request.name().trim());
        preset.setUrl(request.url().trim());
        preset.setMediaType(normalizeMediaType(request.mediaType()));
        preset.setSystemProvided(false);
        preset.setSortOrder(nextSortOrder(preset.getPlacement()));
        preset.setCreatedBy(adminUserId);
        preset.setCreatedAt(now);
        preset.setUpdatedAt(now);
        backgroundPresetMapper.insert(preset);
        return toResponse(preset);
    }

    private Integer nextSortOrder(String placement) {
        BackgroundPreset latest = backgroundPresetMapper.selectList(new LambdaQueryWrapper<BackgroundPreset>()
                        .eq(BackgroundPreset::getPlacement, placement)
                        .orderByDesc(BackgroundPreset::getSortOrder)
                        .orderByDesc(BackgroundPreset::getId)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (latest == null || latest.getSortOrder() == null) {
            return 10;
        }
        return latest.getSortOrder() + 10;
    }

    private String normalizePlacement(String placement) {
        if (placement == null || placement.isBlank() || PLACEMENT_PROFILE.equalsIgnoreCase(placement.trim())) {
            return PLACEMENT_PROFILE;
        }
        throw new BusinessException(400, "Unsupported background placement");
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType != null && MEDIA_TYPE_VIDEO.equalsIgnoreCase(mediaType.trim())) {
            return MEDIA_TYPE_VIDEO;
        }
        if (mediaType != null && MEDIA_TYPE_IMAGE.equalsIgnoreCase(mediaType.trim())) {
            return MEDIA_TYPE_IMAGE;
        }
        throw new BusinessException(400, "Unsupported background media type");
    }

    private void requireRuruAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !RURU_ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BusinessException(403, "Only ruru can manage system backgrounds");
        }
    }

    private BackgroundPresetResponse toResponse(BackgroundPreset preset) {
        return new BackgroundPresetResponse(
                preset.getId(),
                preset.getPlacement(),
                preset.getName(),
                preset.getUrl(),
                preset.getMediaType(),
                Boolean.TRUE.equals(preset.getSystemProvided()),
                preset.getSortOrder(),
                preset.getCreatedAt()
        );
    }
}
