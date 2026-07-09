package com.studyflow.community.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.circle.Circle;
import com.studyflow.community.circle.CircleMapper;
import com.studyflow.community.member.dto.CommunityMemberResponse;
import com.studyflow.community.member.dto.UserProfileRequest;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityMemberService {
    public static final String DEFAULT_CIRCLE_SLUG = "violet-circle";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final CircleMapper circleMapper;
    private final CircleMemberMapper circleMemberMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;

    public CommunityMemberService(
            CircleMapper circleMapper,
            CircleMemberMapper circleMemberMapper,
            UserProfileMapper userProfileMapper,
            UserMapper userMapper
    ) {
        this.circleMapper = circleMapper;
        this.circleMemberMapper = circleMemberMapper;
        this.userProfileMapper = userProfileMapper;
        this.userMapper = userMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void ensureDefaultMembership(Long userId, String username) {
        Circle circle = getDefaultCircle();
        ensureProfile(userId, username);
        ensureCircleMember(circle.getId(), userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommunityMemberResponse getCurrentMember(Long userId) {
        Circle circle = getDefaultCircle();
        CircleMember member = findRequiredMember(circle.getId(), userId);
        User user = findRequiredUser(userId);
        UserProfile profile = findOrCreateProfile(userId, user.getUsername());
        return CommunityMemberResponse.from(circle, member, profile, user.getUsername());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommunityMemberResponse getMember(Long currentUserId, Long targetUserId) {
        Circle circle = getDefaultCircle();
        findRequiredMember(circle.getId(), currentUserId);
        CircleMember targetMember = findRequiredMember(circle.getId(), targetUserId);
        User targetUser = findRequiredUser(targetUserId);
        UserProfile profile = findOrCreateProfile(targetUserId, targetUser.getUsername());
        return CommunityMemberResponse.from(circle, targetMember, profile, targetUser.getUsername());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommunityMemberResponse updateCurrentProfile(Long userId, UserProfileRequest request) {
        Circle circle = getDefaultCircle();
        CircleMember member = findRequiredMember(circle.getId(), userId);
        User user = findRequiredUser(userId);
        UserProfile profile = findOrCreateProfile(userId, user.getUsername());
        profile.setDisplayName(request.displayName());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setSkills(request.skills());
        profile.setGithubUrl(request.githubUrl());
        profile.setWebsiteUrl(request.websiteUrl());
        userProfileMapper.updateById(profile);
        return CommunityMemberResponse.from(circle, member, profile, user.getUsername());
    }

    private Circle getDefaultCircle() {
        Circle circle = circleMapper.selectOne(new LambdaQueryWrapper<Circle>()
                .eq(Circle::getSlug, DEFAULT_CIRCLE_SLUG));
        if (circle == null) {
            throw new BusinessException(500, "默认圈子不存在");
        }
        return circle;
    }

    private void ensureProfile(Long userId, String username) {
        findOrCreateProfile(userId, username);
    }

    // READ_COMMITTED callers can re-read a profile inserted by a concurrent request after a duplicate key race.
    private UserProfile findOrCreateProfile(Long userId, String username) {
        UserProfile profile = findProfile(userId);
        if (profile != null) {
            return profile;
        }

        UserProfile newProfile = new UserProfile();
        newProfile.setUserId(userId);
        newProfile.setDisplayName(username);
        try {
            userProfileMapper.insert(newProfile);
        } catch (DuplicateKeyException ex) {
            UserProfile existingProfile = findProfile(userId);
            if (existingProfile != null) {
                return existingProfile;
            }
            throw ex;
        }
        return newProfile;
    }

    private UserProfile findProfile(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
    }

    private void ensureCircleMember(Long circleId, Long userId) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId));
        if (member != null) {
            return;
        }

        CircleMember newMember = new CircleMember();
        newMember.setCircleId(circleId);
        newMember.setUserId(userId);
        newMember.setRole(ROLE_MEMBER);
        newMember.setStatus(STATUS_ACTIVE);
        circleMemberMapper.insert(newMember);
    }

    private CircleMember findRequiredMember(Long circleId, Long userId) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException(404, "圈子成员不存在");
        }
        return member;
    }

    private User findRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
