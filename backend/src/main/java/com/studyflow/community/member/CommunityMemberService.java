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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityMemberService {
    public static final String DEFAULT_CIRCLE_SLUG = "violet-circle";
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_MUTED = "MUTED";
    public static final String STATUS_DISABLED = "DISABLED";

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
        Circle circle = requireReadableDefaultMember(userId);
        CircleMember member = findRequiredMember(circle.getId(), userId);
        User user = findRequiredUser(userId);
        UserProfile profile = findOrCreateProfile(userId, user.getUsername());
        return CommunityMemberResponse.from(circle, member, profile, user.getUsername());
    }

    public Circle requireDefaultMember(Long userId) {
        return requireReadableDefaultMember(userId);
    }

    public Circle requireReadableDefaultMember(Long userId) {
        Circle circle = getDefaultCircle();
        CircleMember member = findMembership(circle.getId(), userId);
        if (member == null || !(STATUS_ACTIVE.equals(member.getStatus()) || STATUS_MUTED.equals(member.getStatus()))) {
            throw new BusinessException(403, "No permission to access circle content");
        }
        return circle;
    }

    public Circle requireActiveDefaultMember(Long userId) {
        Circle circle = getDefaultCircle();
        CircleMember member = findMembership(circle.getId(), userId);
        if (member == null) {
            throw new BusinessException(403, "No permission to access circle content");
        }
        if (STATUS_MUTED.equals(member.getStatus())) {
            throw new BusinessException(403, "Current account is muted");
        }
        if (!STATUS_ACTIVE.equals(member.getStatus())) {
            throw new BusinessException(403, "No permission to access circle content");
        }
        return circle;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommunityMemberResponse getMember(Long currentUserId, Long targetUserId) {
        Circle circle = requireReadableDefaultMember(currentUserId);
        CircleMember targetMember = findRequiredMember(circle.getId(), targetUserId);
        User targetUser = findRequiredUser(targetUserId);
        UserProfile profile = findOrCreateProfile(targetUserId, targetUser.getUsername());
        return CommunityMemberResponse.from(circle, targetMember, profile, targetUser.getUsername());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<CommunityMemberResponse> listMembers(Long currentUserId) {
        Circle circle = requireReadableDefaultMember(currentUserId);

        List<CircleMember> members = circleMemberMapper.selectList(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circle.getId())
                .orderByAsc(CircleMember::getId));
        List<Long> userIds = members.stream()
                .map(CircleMember::getUserId)
                .toList();
        Map<Long, User> usersById = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, UserProfile> profilesByUserId = userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return members.stream()
                .map(member -> toMemberResponse(
                        circle,
                        member,
                        usersById.get(member.getUserId()),
                        profilesByUserId.get(member.getUserId())
                ))
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommunityMemberResponse updateCurrentProfile(Long userId, UserProfileRequest request) {
        Circle circle = requireActiveDefaultMember(userId);
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
            throw new BusinessException(500, "Default circle does not exist");
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

    private CommunityMemberResponse toMemberResponse(
            Circle circle,
            CircleMember member,
            User user,
            UserProfile profile
    ) {
        if (user == null) {
            throw new BusinessException(404, "User does not exist");
        }
        String displayName = profile != null && profile.getDisplayName() != null
                ? profile.getDisplayName()
                : user.getUsername();
        return new CommunityMemberResponse(
                member.getUserId(),
                user.getUsername(),
                member.getRole(),
                member.getStatus(),
                circle.getId(),
                circle.getName(),
                circle.getSlug(),
                displayName,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getSkills() : null,
                profile != null ? profile.getGithubUrl() : null,
                profile != null ? profile.getWebsiteUrl() : null
        );
    }

    private void ensureCircleMember(Long circleId, Long userId) {
        CircleMember member = findMembership(circleId, userId);
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
        CircleMember member = findMembership(circleId, userId);
        if (member == null) {
            throw new BusinessException(404, "Circle member does not exist");
        }
        return member;
    }

    private CircleMember findMembership(Long circleId, Long userId) {
        return circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getCircleId, circleId)
                .eq(CircleMember::getUserId, userId));
    }

    private User findRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User does not exist");
        }
        return user;
    }
}
