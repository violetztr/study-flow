package com.studyflow.community.topic;

import com.studyflow.common.ApiResponse;
import com.studyflow.community.topic.dto.CommunityTopicResponse;
import com.studyflow.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/community/topics")
public class CommunityTopicController {
    private final CommunityTopicService communityTopicService;

    public CommunityTopicController(CommunityTopicService communityTopicService) {
        this.communityTopicService = communityTopicService;
    }

    @GetMapping
    public ApiResponse<List<CommunityTopicResponse>> listTopics(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(communityTopicService.listTopics(currentUserId(principal)));
    }

    private Long currentUserId(UserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }
}
