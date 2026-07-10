package com.studyflow.community.topic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.community.topic.dto.CommunityTopicResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunityTopicService {
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CommunityMemberService communityMemberService;
    private final CommunityTopicMapper communityTopicMapper;

    public CommunityTopicService(CommunityMemberService communityMemberService, CommunityTopicMapper communityTopicMapper) {
        this.communityMemberService = communityMemberService;
        this.communityTopicMapper = communityTopicMapper;
    }

    public List<CommunityTopicResponse> listTopics(Long userId) {
        Circle circle = communityMemberService.requireReadableDefaultMember(userId);
        return communityTopicMapper.selectList(new LambdaQueryWrapper<CommunityTopic>()
                        .eq(CommunityTopic::getCircleId, circle.getId())
                        .eq(CommunityTopic::getStatus, STATUS_ACTIVE)
                        .orderByAsc(CommunityTopic::getSortOrder)
                        .orderByAsc(CommunityTopic::getId))
                .stream()
                .map(this::toResponse)
                .toList();
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
