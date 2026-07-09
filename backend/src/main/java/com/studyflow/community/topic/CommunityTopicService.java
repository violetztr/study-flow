package com.studyflow.community.topic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.circle.CircleMapper;
import com.studyflow.community.topic.dto.CommunityTopicResponse;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.studyflow.community.member.CommunityMemberService.DEFAULT_CIRCLE_SLUG;

@Service
public class CommunityTopicService {
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CircleMapper circleMapper;
    private final CommunityTopicMapper communityTopicMapper;

    public CommunityTopicService(CircleMapper circleMapper, CommunityTopicMapper communityTopicMapper) {
        this.circleMapper = circleMapper;
        this.communityTopicMapper = communityTopicMapper;
    }

    public List<CommunityTopicResponse> listTopics(Long userId) {
        Circle circle = getDefaultCircle();
        return communityTopicMapper.selectList(new LambdaQueryWrapper<CommunityTopic>()
                        .eq(CommunityTopic::getCircleId, circle.getId())
                        .eq(CommunityTopic::getStatus, STATUS_ACTIVE)
                        .orderByAsc(CommunityTopic::getSortOrder)
                        .orderByAsc(CommunityTopic::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Circle getDefaultCircle() {
        Circle circle = circleMapper.selectOne(new LambdaQueryWrapper<Circle>()
                .eq(Circle::getSlug, DEFAULT_CIRCLE_SLUG));
        if (circle == null) {
            throw new BusinessException(500, "默认圈子不存在");
        }
        return circle;
    }

    private CommunityTopicResponse toResponse(CommunityTopic topic) {
        return new CommunityTopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getColor(),
                topic.getPostCount()
        );
    }
}
