# Violet Circle Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Violet Circle community loop: default circle membership, topics, posts, comments, likes, member profiles, moderation basics, and a React entry point.

**Architecture:** Keep the current Spring Boot + React application as a modular monolith. Add a focused `com.studyflow.community` backend package and current-style frontend pages/API files without deleting the existing learning, notes, daily, and project tools.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security JWT, MyBatis-Plus, Flyway, MySQL/H2 test profile, React 19, TypeScript, Ant Design, TanStack Query, Docker.

---

## Scope Check

This plan implements only the first community release. It does not implement image upload, private messages, real-time chat, full-text search, multi-circle UI, recommendation algorithms, or end-to-end encrypted messaging.

The first release will still reserve database fields such as `circle_id`, `parent_id`, `status`, and generic reaction targets so later modules can be added without rewriting the foundation.

## Current Codebase Facts

- Backend code is under `backend/src/main/java/com/studyflow`.
- Database migrations live in `backend/src/main/resources/db/migration`.
- Backend tests use `@SpringBootTest`, `@AutoConfigureMockMvc`, and H2 in MySQL mode.
- Existing authenticated endpoints use `@AuthenticationPrincipal UserPrincipal`.
- Existing API responses use `ApiResponse.success(...)` with numeric `code = 0`.
- Frontend pages are currently under `frontend/src/pages`.
- Frontend API clients are currently under `frontend/src/api`.
- Frontend routing is centralized in `frontend/src/App.tsx`.
- The app layout is centralized in `frontend/src/layouts/AppLayout.tsx`.

## File Structure

### Backend Files To Create

- `backend/src/main/resources/db/migration/V6__add_violet_circle_community.sql`: user role/status columns and community tables.
- `backend/src/main/java/com/studyflow/community/circle/Circle.java`: maps `circles`.
- `backend/src/main/java/com/studyflow/community/circle/CircleMapper.java`: MyBatis mapper for circles.
- `backend/src/main/java/com/studyflow/community/member/CircleMember.java`: maps `circle_members`.
- `backend/src/main/java/com/studyflow/community/member/CircleMemberMapper.java`: MyBatis mapper for members.
- `backend/src/main/java/com/studyflow/community/member/UserProfile.java`: maps `user_profiles`.
- `backend/src/main/java/com/studyflow/community/member/UserProfileMapper.java`: MyBatis mapper for profiles.
- `backend/src/main/java/com/studyflow/community/member/CommunityMemberService.java`: joins users to the default circle and returns member/profile data.
- `backend/src/main/java/com/studyflow/community/member/CommunityMemberController.java`: current member and member list APIs.
- `backend/src/main/java/com/studyflow/community/member/dto/CommunityMemberResponse.java`: member response DTO.
- `backend/src/main/java/com/studyflow/community/member/dto/UserProfileRequest.java`: profile update request DTO.
- `backend/src/main/java/com/studyflow/community/topic/CommunityTopic.java`: maps `community_topics`.
- `backend/src/main/java/com/studyflow/community/topic/CommunityTopicMapper.java`: topic mapper.
- `backend/src/main/java/com/studyflow/community/topic/CommunityTopicService.java`: topic query service.
- `backend/src/main/java/com/studyflow/community/topic/CommunityTopicController.java`: topic list API.
- `backend/src/main/java/com/studyflow/community/topic/dto/CommunityTopicResponse.java`: topic response DTO.
- `backend/src/main/java/com/studyflow/community/post/CommunityPost.java`: maps `community_posts`.
- `backend/src/main/java/com/studyflow/community/post/CommunityPostMapper.java`: post mapper.
- `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`: post business rules.
- `backend/src/main/java/com/studyflow/community/post/CommunityPostController.java`: post and feed APIs.
- `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostRequest.java`: create/update post request.
- `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostResponse.java`: post response DTO.
- `backend/src/main/java/com/studyflow/community/comment/CommunityComment.java`: maps `community_comments`.
- `backend/src/main/java/com/studyflow/community/comment/CommunityCommentMapper.java`: comment mapper.
- `backend/src/main/java/com/studyflow/community/comment/CommunityCommentService.java`: comment business rules.
- `backend/src/main/java/com/studyflow/community/comment/CommunityCommentController.java`: comment APIs.
- `backend/src/main/java/com/studyflow/community/comment/dto/CommunityCommentRequest.java`: comment request DTO.
- `backend/src/main/java/com/studyflow/community/comment/dto/CommunityCommentResponse.java`: comment response DTO.
- `backend/src/main/java/com/studyflow/community/reaction/CommunityReaction.java`: maps `community_reactions`.
- `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionMapper.java`: reaction mapper.
- `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionService.java`: like/unlike rules.
- `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionController.java`: reaction APIs.
- `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationAction.java`: maps `community_moderation_actions`.
- `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationActionMapper.java`: moderation action mapper.
- `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationService.java`: admin actions.
- `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationController.java`: admin APIs.
- `backend/src/main/java/com/studyflow/community/moderation/dto/ModerationRequest.java`: optional moderation reason.
- `backend/src/test/java/com/studyflow/community/CommunityFoundationControllerTest.java`: membership/profile/topic tests.
- `backend/src/test/java/com/studyflow/community/CommunityPostControllerTest.java`: post/feed tests.
- `backend/src/test/java/com/studyflow/community/CommunityCommentControllerTest.java`: comment tests.
- `backend/src/test/java/com/studyflow/community/CommunityReactionControllerTest.java`: like/unlike tests.
- `backend/src/test/java/com/studyflow/community/CommunityModerationControllerTest.java`: admin permission/moderation tests.

### Backend Files To Modify

- `backend/src/main/java/com/studyflow/user/User.java`: add `role` and `status`.
- `backend/src/main/java/com/studyflow/user/dto/UserResponse.java`: return `role` and `status`.
- `backend/src/main/java/com/studyflow/auth/AuthService.java`: create default membership/profile after registration and reject disabled login.
- `backend/src/main/java/com/studyflow/security/JwtAuthenticationFilter.java`: keep existing behavior unless user status checks need central enforcement after Task 5.
- `backend/src/main/java/com/studyflow/security/UserPrincipal.java`: add role/status only if admin code needs principal-level access.
- `backend/src/main/java/com/studyflow/common/GlobalExceptionHandler.java`: keep current error format and only adjust if new validation errors need consistent mapping.

### Frontend Files To Create

- `frontend/src/api/community.ts`: typed community API functions.
- `frontend/src/pages/CircleFeedPage.tsx`: community homepage/feed.
- `frontend/src/pages/CreatePostPage.tsx`: create post page.
- `frontend/src/pages/PostDetailPage.tsx`: post detail, comments, likes.
- `frontend/src/pages/MembersPage.tsx`: community members.
- `frontend/src/pages/MemberProfilePage.tsx`: member profile.
- `frontend/src/pages/CommunityAdminPage.tsx`: simple moderation dashboard.
- `frontend/src/components/community/PostCard.tsx`: feed card.
- `frontend/src/components/community/PostComposer.tsx`: post form.
- `frontend/src/components/community/CommentList.tsx`: comments display.
- `frontend/src/components/community/TopicBadge.tsx`: topic display.

### Frontend Files To Modify

- `frontend/src/App.tsx`: route `/` to `/circle`, add community routes.
- `frontend/src/layouts/AppLayout.tsx`: add navigation entries for circle, learning, notes, daily, members, admin.
- `frontend/src/pages/LoginPage.tsx`: after login, navigate to `/circle` if currently navigating to `/dashboard`.
- `frontend/src/pages/RegisterPage.tsx`: after registration, keep the existing login flow or navigate to login with community wording.
- `frontend/src/index.css`: add community page styling only if component-level Ant Design props are not enough.

### Documentation Files To Modify

- `README.md`: add Violet Circle community feature summary.
- `docs/API.md`: document new community endpoints if this file exists.
- `docs/DATABASE.md`: document new community tables if this file exists.
- `docs/DEPLOY.md`: mention migration/deploy check if this file exists.

If a documentation file listed above does not exist, update the closest existing documentation file that currently describes API, database, or deployment.

## Task 1: Community Foundation, User Role, Default Membership

**Files:**

- Create: `backend/src/main/resources/db/migration/V6__add_violet_circle_community.sql`
- Create: `backend/src/main/java/com/studyflow/community/circle/Circle.java`
- Create: `backend/src/main/java/com/studyflow/community/circle/CircleMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/member/CircleMember.java`
- Create: `backend/src/main/java/com/studyflow/community/member/CircleMemberMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/member/UserProfile.java`
- Create: `backend/src/main/java/com/studyflow/community/member/UserProfileMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/member/CommunityMemberService.java`
- Create: `backend/src/main/java/com/studyflow/community/member/CommunityMemberController.java`
- Create: `backend/src/main/java/com/studyflow/community/member/dto/CommunityMemberResponse.java`
- Create: `backend/src/main/java/com/studyflow/community/member/dto/UserProfileRequest.java`
- Modify: `backend/src/main/java/com/studyflow/user/User.java`
- Modify: `backend/src/main/java/com/studyflow/user/dto/UserResponse.java`
- Modify: `backend/src/main/java/com/studyflow/auth/AuthService.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityFoundationControllerTest.java`

- [ ] **Step 1: Write the failing foundation tests**

Create `CommunityFoundationControllerTest` with these behaviors:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityFoundationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerCreatesDefaultCommunityMembership() throws Exception {
        String token = registerAndLogin("circle_foundation_alice", "circle_foundation_alice@example.com");

        mockMvc.perform(get("/api/community/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("circle_foundation_alice"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.circleSlug").value("violet-circle"));
    }

    @Test
    void updateProfileChangesCurrentMemberDisplayName() throws Exception {
        String token = registerAndLogin("circle_profile_alice", "circle_profile_alice@example.com");

        mockMvc.perform(put("/api/community/members/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Alice Circle",
                                  "bio": "Learning full stack step by step",
                                  "skills": "Java,React,Docker",
                                  "githubUrl": "https://github.com/alice",
                                  "websiteUrl": "https://example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Alice Circle"))
                .andExpect(jsonPath("$.data.bio").value("Learning full stack step by step"));
    }

    private String registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return loginResult.getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
cd backend
mvn -Dtest=CommunityFoundationControllerTest test
```

Expected: compilation fails because community member classes and routes do not exist.

- [ ] **Step 3: Add migration `V6__add_violet_circle_community.sql`**

Use MySQL-compatible SQL that also works in H2 MySQL mode:

```sql
ALTER TABLE users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE circles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    visibility VARCHAR(40) NOT NULL DEFAULT 'PUBLIC_REGISTERED',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_circles_slug (slug)
);

CREATE TABLE circle_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_circle_members_circle_user (circle_id, user_id),
    INDEX idx_circle_members_user_id (user_id),
    INDEX idx_circle_members_circle_id (circle_id)
);

CREATE TABLE user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(80),
    bio VARCHAR(500),
    avatar_url VARCHAR(500),
    skills VARCHAR(500),
    github_url VARCHAR(300),
    website_url VARCHAR(300),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_profiles_user_id (user_id)
);

CREATE TABLE community_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(30) NOT NULL DEFAULT '#2f6f60',
    sort_order INT NOT NULL DEFAULT 0,
    post_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_community_topics_circle_slug (circle_id, slug),
    INDEX idx_community_topics_circle_id (circle_id)
);

CREATE TABLE community_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    topic_id BIGINT,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    visibility VARCHAR(30) NOT NULL DEFAULT 'CIRCLE',
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    comment_count INT NOT NULL DEFAULT 0,
    reaction_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    last_activity_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_community_posts_circle_activity (circle_id, pinned, last_activity_at),
    INDEX idx_community_posts_author_id (author_id),
    INDEX idx_community_posts_topic_id (topic_id)
);

CREATE TABLE community_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    reaction_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_community_comments_post_id (post_id),
    INDEX idx_community_comments_author_id (author_id)
);

CREATE TABLE community_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(30) NOT NULL DEFAULT 'LIKE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_community_reactions_target_user (circle_id, target_type, target_id, user_id, reaction_type),
    INDEX idx_community_reactions_user_id (user_id)
);

CREATE TABLE community_moderation_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_community_moderation_actions_target (target_type, target_id),
    INDEX idx_community_moderation_actions_admin (admin_user_id)
);

INSERT INTO circles (name, slug, description)
VALUES ('Violet Circle', 'violet-circle', 'A small community for friends to learn, share, and grow together');

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '学习', 'learning', '学习进度、技术问题、读书记录', '#2f6f60', 10 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '笔记', 'notes', '知识沉淀、教程、灵感整理', '#8a5a44', 20 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '日常', 'daily', '生活记录、碎碎念、日常打卡', '#4f6f8f', 30 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '项目', 'projects', '作品展示、项目复盘、协作想法', '#6f5f2f', 40 FROM circles WHERE slug = 'violet-circle';
```

- [ ] **Step 4: Add `role` and `status` to `User` and `UserResponse`**

Add fields to `User`:

```java
private String role;
private String status;
```

Add getters and setters.

Update `UserResponse` so API clients can see role/status:

```java
public record UserResponse(Long id, String username, String email, String role, String status) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}
```

- [ ] **Step 5: Add foundation models and mappers**

Each mapper follows existing project style:

```java
@Mapper
public interface CircleMapper extends BaseMapper<Circle> {
}
```

Models use `@TableName` and `@TableId(type = IdType.AUTO)`. Include fields matching SQL names with camelCase Java fields.

- [ ] **Step 6: Implement `CommunityMemberService`**

Required methods:

```java
public static final String DEFAULT_CIRCLE_SLUG = "violet-circle";

@Transactional
public void ensureDefaultMembership(Long userId, String username) {
    Circle circle = getDefaultCircle();
    ensureProfile(userId, username);
    ensureCircleMember(circle.getId(), userId);
}

public CommunityMemberResponse getCurrentMember(Long userId) {
    Circle circle = getDefaultCircle();
    CircleMember member = findRequiredMember(circle.getId(), userId);
    UserProfile profile = findOrCreateProfile(userId, null);
    return CommunityMemberResponse.from(circle, member, profile);
}

public CommunityMemberResponse getMember(Long currentUserId, Long targetUserId) {
    Circle circle = getDefaultCircle();
    findRequiredMember(circle.getId(), currentUserId);
    CircleMember targetMember = findRequiredMember(circle.getId(), targetUserId);
    UserProfile profile = findOrCreateProfile(targetUserId, null);
    return CommunityMemberResponse.from(circle, targetMember, profile);
}

@Transactional
public CommunityMemberResponse updateCurrentProfile(Long userId, UserProfileRequest request) {
    Circle circle = getDefaultCircle();
    CircleMember member = findRequiredMember(circle.getId(), userId);
    UserProfile profile = findOrCreateProfile(userId, null);
    profile.setDisplayName(request.displayName());
    profile.setBio(request.bio());
    profile.setSkills(request.skills());
    profile.setGithubUrl(request.githubUrl());
    profile.setWebsiteUrl(request.websiteUrl());
    userProfileMapper.updateById(profile);
    return CommunityMemberResponse.from(circle, member, profile);
}
```

Business rules:

- If the default circle is missing, throw `BusinessException(500, "默认圈子不存在")`.
- If profile is missing, create one with `displayName = username`.
- If membership is missing, create one with role `MEMBER` and status `ACTIVE`.

- [ ] **Step 7: Modify registration to create membership**

In `AuthService.register`, after `userMapper.insert(user)`, call:

```java
communityMemberService.ensureDefaultMembership(user.getId(), user.getUsername());
```

Also set user defaults before insert:

```java
user.setRole("MEMBER");
user.setStatus("ACTIVE");
```

Constructor now includes `CommunityMemberService`.

- [ ] **Step 8: Add member controller**

Routes:

```java
@GetMapping("/me")
public ApiResponse<CommunityMemberResponse> me(@AuthenticationPrincipal UserPrincipal principal)

@GetMapping("/{userId}")
public ApiResponse<CommunityMemberResponse> getMember(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long userId
)

@PutMapping("/me/profile")
public ApiResponse<CommunityMemberResponse> updateProfile(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody UserProfileRequest request
)
```

Controller base path:

```java
@RequestMapping("/api/community/members")
```

- [ ] **Step 9: Run the foundation test**

Run:

```powershell
cd backend
mvn -Dtest=CommunityFoundationControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10: Commit Task 1**

Run:

```powershell
git add backend/src/main/resources/db/migration/V6__add_violet_circle_community.sql backend/src/main/java/com/studyflow/user backend/src/main/java/com/studyflow/auth/AuthService.java backend/src/main/java/com/studyflow/community backend/src/test/java/com/studyflow/community/CommunityFoundationControllerTest.java
git commit -m "feat: add violet circle community foundation"
```

## Task 2: Topics, Posts, And Community Feed

**Files:**

- Create: `backend/src/main/java/com/studyflow/community/topic/CommunityTopicService.java`
- Create: `backend/src/main/java/com/studyflow/community/topic/CommunityTopicController.java`
- Create: `backend/src/main/java/com/studyflow/community/topic/dto/CommunityTopicResponse.java`
- Create: `backend/src/main/java/com/studyflow/community/post/CommunityPost.java`
- Create: `backend/src/main/java/com/studyflow/community/post/CommunityPostMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- Create: `backend/src/main/java/com/studyflow/community/post/CommunityPostController.java`
- Create: `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostRequest.java`
- Create: `backend/src/main/java/com/studyflow/community/post/dto/CommunityPostResponse.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityPostControllerTest.java`

- [ ] **Step 1: Write failing post/feed tests**

Required tests:

```java
@Test
void createPostReturnsPostAndFeedShowsIt() throws Exception {
    String token = registerAndLogin("post_author_alice", "post_author_alice@example.com");
    Long topicId = firstTopicId(token);

    Long postId = createPost(token, topicId, "第一条社区帖子", "今天开始把学习记录发到 Violet Circle。");

    mockMvc.perform(get("/api/community/feed")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(postId))
            .andExpect(jsonPath("$.data[0].title").value("第一条社区帖子"))
            .andExpect(jsonPath("$.data[0].commentCount").value(0))
            .andExpect(jsonPath("$.data[0].reactionCount").value(0));
}

@Test
void anotherUserCannotUpdatePost() throws Exception {
    String aliceToken = registerAndLogin("post_owner_alice", "post_owner_alice@example.com");
    String bobToken = registerAndLogin("post_owner_bob", "post_owner_bob@example.com");
    Long topicId = firstTopicId(aliceToken);
    Long postId = createPost(aliceToken, topicId, "Alice 的帖子", "只有 Alice 能改。");

    mockMvc.perform(put("/api/community/posts/{id}", postId)
                    .header("Authorization", "Bearer " + bobToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "topicId": %d,
                              "title": "Bob 想改",
                              "content": "这应该失败。"
                            }
                            """.formatted(topicId)))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run failing post tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityPostControllerTest test
```

Expected: compilation fails because post/topic classes and routes do not exist.

- [ ] **Step 3: Implement topic list**

Route:

```text
GET /api/community/topics
```

Behavior:

- Requires login.
- Returns active topics for default circle.
- Sort by `sort_order ASC`, then `id ASC`.

Response fields:

```java
public record CommunityTopicResponse(
        Long id,
        String name,
        String slug,
        String description,
        String color,
        Integer postCount
) {
}
```

- [ ] **Step 4: Implement post request/response DTOs**

Request:

```java
public record CommunityPostRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank String content,
        Long topicId
) {
}
```

Response:

```java
public record CommunityPostResponse(
        Long id,
        Long circleId,
        Long authorId,
        String authorName,
        Long topicId,
        String topicName,
        String title,
        String content,
        String status,
        Boolean pinned,
        Integer commentCount,
        Integer reactionCount,
        Integer viewCount,
        Boolean likedByCurrentUser,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

- [ ] **Step 5: Implement `CommunityPostService`**

Required methods:

```java
public List<CommunityPostResponse> listFeed(Long userId)
public CommunityPostResponse getPost(Long userId, Long postId)
@Transactional public CommunityPostResponse createPost(Long userId, CommunityPostRequest request)
@Transactional public CommunityPostResponse updatePost(Long userId, Long postId, CommunityPostRequest request)
@Transactional public void deletePost(Long userId, Long postId)
```

Rules:

- `listFeed` only returns `PUBLISHED` posts.
- Feed order is `pinned DESC`, `last_activity_at DESC`, `created_at DESC`.
- `createPost` requires current member status `ACTIVE`.
- If topicId is present, topic must exist in default circle and status `ACTIVE`.
- Update/delete requires `author_id = current user`.
- Delete sets `status = DELETED` and `deleted_at = now`, not physical deletion.
- If ownership fails, throw `BusinessException(403, "没有权限操作这条帖子")`.

- [ ] **Step 6: Implement `CommunityPostController`**

Routes:

```text
GET /api/community/feed
POST /api/community/posts
GET /api/community/posts/{postId}
PUT /api/community/posts/{postId}
DELETE /api/community/posts/{postId}
```

Use `@AuthenticationPrincipal UserPrincipal principal` for all routes.

- [ ] **Step 7: Run post tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityPostControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit Task 2**

Run:

```powershell
git add backend/src/main/java/com/studyflow/community/topic backend/src/main/java/com/studyflow/community/post backend/src/test/java/com/studyflow/community/CommunityPostControllerTest.java
git commit -m "feat: add violet circle posts and feed"
```

## Task 3: Comments

**Files:**

- Create: `backend/src/main/java/com/studyflow/community/comment/CommunityComment.java`
- Create: `backend/src/main/java/com/studyflow/community/comment/CommunityCommentMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/comment/CommunityCommentService.java`
- Create: `backend/src/main/java/com/studyflow/community/comment/CommunityCommentController.java`
- Create: `backend/src/main/java/com/studyflow/community/comment/dto/CommunityCommentRequest.java`
- Create: `backend/src/main/java/com/studyflow/community/comment/dto/CommunityCommentResponse.java`
- Modify: `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityCommentControllerTest.java`

- [ ] **Step 1: Write failing comment tests**

Required tests:

```java
@Test
void addCommentIncrementsPostCommentCount() throws Exception {
    String token = registerAndLogin("comment_alice", "comment_alice@example.com");
    Long topicId = firstTopicId(token);
    Long postId = createPost(token, topicId, "评论测试", "这条帖子会收到评论。");

    mockMvc.perform(post("/api/community/posts/{postId}/comments", postId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "content": "第一条评论"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("第一条评论"));

    mockMvc.perform(get("/api/community/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.commentCount").value(1));
}

@Test
void anotherUserCannotDeleteComment() throws Exception {
    String aliceToken = registerAndLogin("comment_owner_alice", "comment_owner_alice@example.com");
    String bobToken = registerAndLogin("comment_owner_bob", "comment_owner_bob@example.com");
    Long topicId = firstTopicId(aliceToken);
    Long postId = createPost(aliceToken, topicId, "评论权限", "Bob 不能删 Alice 的评论。");
    Long commentId = createComment(aliceToken, postId, "Alice 的评论");

    mockMvc.perform(delete("/api/community/comments/{commentId}", commentId)
                    .header("Authorization", "Bearer " + bobToken))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run failing comment tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityCommentControllerTest test
```

Expected: compilation fails because comment classes and routes do not exist.

- [ ] **Step 3: Implement comment DTOs**

Request:

```java
public record CommunityCommentRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
```

Response:

```java
public record CommunityCommentResponse(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String content,
        String status,
        Integer reactionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

- [ ] **Step 4: Implement `CommunityCommentService`**

Required methods:

```java
public List<CommunityCommentResponse> listComments(Long userId, Long postId)
@Transactional public CommunityCommentResponse createComment(Long userId, Long postId, CommunityCommentRequest request)
@Transactional public void deleteComment(Long userId, Long commentId)
```

Rules:

- Post must exist and be `PUBLISHED`.
- Current member must be `ACTIVE`.
- First version only creates top-level comments with `parentId = null`.
- Delete requires comment author.
- Delete sets comment `status = DELETED` and `deleted_at = now`.
- Create increments `community_posts.comment_count`.
- Create updates `community_posts.last_activity_at`.
- Delete decrements `comment_count` but never below zero.

- [ ] **Step 5: Implement comment controller**

Routes:

```text
GET /api/community/posts/{postId}/comments
POST /api/community/posts/{postId}/comments
DELETE /api/community/comments/{commentId}
```

- [ ] **Step 6: Run comment tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityCommentControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit Task 3**

Run:

```powershell
git add backend/src/main/java/com/studyflow/community/comment backend/src/main/java/com/studyflow/community/post backend/src/test/java/com/studyflow/community/CommunityCommentControllerTest.java
git commit -m "feat: add violet circle comments"
```

## Task 4: Likes With Generic Reactions

**Files:**

- Create: `backend/src/main/java/com/studyflow/community/reaction/CommunityReaction.java`
- Create: `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionService.java`
- Create: `backend/src/main/java/com/studyflow/community/reaction/CommunityReactionController.java`
- Modify: `backend/src/main/java/com/studyflow/community/post/CommunityPostService.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityReactionControllerTest.java`

- [ ] **Step 1: Write failing reaction tests**

Required tests:

```java
@Test
void likePostIsIdempotentAndUpdatesCount() throws Exception {
    String token = registerAndLogin("reaction_alice", "reaction_alice@example.com");
    Long topicId = firstTopicId(token);
    Long postId = createPost(token, topicId, "点赞测试", "重复点赞只能算一次。");

    likePost(token, postId);
    likePost(token, postId);

    mockMvc.perform(get("/api/community/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reactionCount").value(1))
            .andExpect(jsonPath("$.data.likedByCurrentUser").value(true));
}

@Test
void unlikePostUpdatesCount() throws Exception {
    String token = registerAndLogin("reaction_bob", "reaction_bob@example.com");
    Long topicId = firstTopicId(token);
    Long postId = createPost(token, topicId, "取消点赞测试", "取消后计数为 0。");

    likePost(token, postId);

    mockMvc.perform(delete("/api/community/posts/{postId}/reactions/like", postId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

    mockMvc.perform(get("/api/community/posts/{postId}", postId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reactionCount").value(0))
            .andExpect(jsonPath("$.data.likedByCurrentUser").value(false));
}
```

- [ ] **Step 2: Run failing reaction tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityReactionControllerTest test
```

Expected: compilation fails because reaction classes and routes do not exist.

- [ ] **Step 3: Implement `CommunityReactionService`**

Constants:

```java
private static final String TARGET_POST = "POST";
private static final String REACTION_LIKE = "LIKE";
```

Required methods:

```java
@Transactional public void likePost(Long userId, Long postId)
@Transactional public void unlikePost(Long userId, Long postId)
public boolean hasLikedPost(Long userId, Long postId)
```

Rules:

- Post must exist and be `PUBLISHED`.
- Current member must be `ACTIVE`.
- Like is idempotent: if reaction row exists, do not insert a second row and do not increment count.
- Unlike is idempotent: if row is missing, return success and do not decrement count.
- Count never drops below zero.

- [ ] **Step 4: Update post response to include `likedByCurrentUser`**

In `CommunityPostService`, calculate `likedByCurrentUser` using `CommunityReactionService.hasLikedPost`.

Avoid circular dependency by keeping the read check simple. If constructor cycles appear, move `hasLikedPost` query into a small helper or mapper query owned by the post service.

- [ ] **Step 5: Add reaction controller**

Routes:

```text
POST /api/community/posts/{postId}/reactions/like
DELETE /api/community/posts/{postId}/reactions/like
```

Both return `ApiResponse.success()`.

- [ ] **Step 6: Run reaction tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityReactionControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit Task 4**

Run:

```powershell
git add backend/src/main/java/com/studyflow/community/reaction backend/src/main/java/com/studyflow/community/post backend/src/test/java/com/studyflow/community/CommunityReactionControllerTest.java
git commit -m "feat: add violet circle post likes"
```

## Task 5: Moderation And Admin Rules

**Files:**

- Create: `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationAction.java`
- Create: `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationActionMapper.java`
- Create: `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationService.java`
- Create: `backend/src/main/java/com/studyflow/community/moderation/CommunityModerationController.java`
- Create: `backend/src/main/java/com/studyflow/community/moderation/dto/ModerationRequest.java`
- Modify: `backend/src/main/java/com/studyflow/community/member/CommunityMemberService.java`
- Modify: `backend/src/main/java/com/studyflow/auth/AuthService.java`
- Test: `backend/src/test/java/com/studyflow/community/CommunityModerationControllerTest.java`

- [ ] **Step 1: Write failing moderation tests**

Required tests:

```java
@Test
void memberCannotHidePost() throws Exception {
    String aliceToken = registerAndLogin("moderation_member_alice", "moderation_member_alice@example.com");
    Long topicId = firstTopicId(aliceToken);
    Long postId = createPost(aliceToken, topicId, "普通用户不能隐藏", "管理员才可以隐藏帖子。");

    mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                    .header("Authorization", "Bearer " + aliceToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "reason": "member should fail"
                            }
                            """))
            .andExpect(status().isForbidden());
}

@Test
void adminCanHideAndRestorePost() throws Exception {
    String adminToken = registerAdminAndLogin("moderation_admin", "moderation_admin@example.com");
    String memberToken = registerAndLogin("moderation_author", "moderation_author@example.com");
    Long topicId = firstTopicId(memberToken);
    Long postId = createPost(memberToken, topicId, "管理员审核", "这条帖子会被隐藏再恢复。");

    mockMvc.perform(post("/api/admin/community/posts/{postId}/hide", postId)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "reason": "测试隐藏"
                            }
                            """))
            .andExpect(status().isOk());

    mockMvc.perform(get("/api/community/feed")
                    .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));

    mockMvc.perform(post("/api/admin/community/posts/{postId}/restore", postId)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "reason": "测试恢复"
                            }
                            """))
            .andExpect(status().isOk());

    mockMvc.perform(get("/api/community/feed")
                    .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)));
}
```

The test helper `registerAdminAndLogin` can register a normal user and then use `UserMapper` inside the test to set `role = ADMIN`.

- [ ] **Step 2: Run failing moderation tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityModerationControllerTest test
```

Expected: compilation fails because moderation classes and routes do not exist.

- [ ] **Step 3: Implement admin role check**

In `CommunityModerationService`, load the current user by ID and require:

```java
"ADMIN".equals(user.getRole()) || "OWNER".equals(user.getRole())
```

If not admin, throw:

```java
throw new BusinessException(403, "需要管理员权限");
```

- [ ] **Step 4: Implement moderation actions**

Routes:

```text
POST /api/admin/community/posts/{postId}/hide
POST /api/admin/community/posts/{postId}/restore
POST /api/admin/community/comments/{commentId}/hide
POST /api/admin/community/comments/{commentId}/restore
POST /api/admin/community/members/{userId}/mute
POST /api/admin/community/members/{userId}/unmute
```

Rules:

- Hide post: set post `status = HIDDEN`.
- Restore post: set post `status = PUBLISHED`.
- Hide comment: set comment `status = HIDDEN`.
- Restore comment: set comment `status = PUBLISHED`.
- Mute member: set `circle_members.status = MUTED`.
- Unmute member: set `circle_members.status = ACTIVE`.
- Every admin action inserts a row into `community_moderation_actions`.

- [ ] **Step 5: Make muted users read-only**

Before creating posts or comments, require current `circle_members.status = ACTIVE`.

If status is `MUTED`, throw:

```java
throw new BusinessException(403, "当前账号已被禁言");
```

- [ ] **Step 6: Reject disabled login**

In `AuthService.login`, after the user is found and password matches:

```java
if ("DISABLED".equals(user.getStatus())) {
    throw new BusinessException(403, "账号已被禁用");
}
```

- [ ] **Step 7: Run moderation tests**

Run:

```powershell
cd backend
mvn -Dtest=CommunityModerationControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Run all backend tests**

Run:

```powershell
cd backend
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 9: Commit Task 5**

Run:

```powershell
git add backend/src/main/java/com/studyflow/community backend/src/main/java/com/studyflow/auth/AuthService.java backend/src/test/java/com/studyflow/community/CommunityModerationControllerTest.java
git commit -m "feat: add violet circle moderation"
```

## Task 6: Frontend Community Entry And Pages

**Files:**

- Create: `frontend/src/api/community.ts`
- Create: `frontend/src/components/community/PostCard.tsx`
- Create: `frontend/src/components/community/PostComposer.tsx`
- Create: `frontend/src/components/community/CommentList.tsx`
- Create: `frontend/src/components/community/TopicBadge.tsx`
- Create: `frontend/src/pages/CircleFeedPage.tsx`
- Create: `frontend/src/pages/CreatePostPage.tsx`
- Create: `frontend/src/pages/PostDetailPage.tsx`
- Create: `frontend/src/pages/MembersPage.tsx`
- Create: `frontend/src/pages/MemberProfilePage.tsx`
- Create: `frontend/src/pages/CommunityAdminPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/layouts/AppLayout.tsx`
- Modify: `frontend/src/pages/LoginPage.tsx`

- [ ] **Step 1: Add typed community API client**

`frontend/src/api/community.ts` exports:

```ts
import { http } from './http'

export type CommunityTopic = {
  id: number
  name: string
  slug: string
  description?: string
  color: string
  postCount: number
}

export type CommunityPost = {
  id: number
  authorId: number
  authorName: string
  topicId?: number
  topicName?: string
  title: string
  content: string
  status: string
  pinned: boolean
  commentCount: number
  reactionCount: number
  viewCount: number
  likedByCurrentUser: boolean
  createdAt: string
  updatedAt: string
}

export type CommunityComment = {
  id: number
  postId: number
  authorId: number
  authorName: string
  content: string
  status: string
  reactionCount: number
  createdAt: string
}

export type CommunityMember = {
  userId: number
  username: string
  displayName: string
  bio?: string
  skills?: string
  role: string
  memberStatus: string
  circleSlug: string
}

export type CommunityPostPayload = {
  title: string
  content: string
  topicId?: number
}

export type CommunityCommentPayload = {
  content: string
}

export const communityApi = {
  listFeed: () => http.get<CommunityPost[]>('/community/feed'),
  listTopics: () => http.get<CommunityTopic[]>('/community/topics'),
  createPost: (payload: CommunityPostPayload) => http.post<CommunityPost>('/community/posts', payload),
  getPost: (postId: number) => http.get<CommunityPost>(`/community/posts/${postId}`),
  updatePost: (postId: number, payload: CommunityPostPayload) => http.put<CommunityPost>(`/community/posts/${postId}`, payload),
  deletePost: (postId: number) => http.delete<void>(`/community/posts/${postId}`),
  listComments: (postId: number) => http.get<CommunityComment[]>(`/community/posts/${postId}/comments`),
  createComment: (postId: number, payload: CommunityCommentPayload) => http.post<CommunityComment>(`/community/posts/${postId}/comments`, payload),
  likePost: (postId: number) => http.post<void>(`/community/posts/${postId}/reactions/like`),
  unlikePost: (postId: number) => http.delete<void>(`/community/posts/${postId}/reactions/like`),
  getMe: () => http.get<CommunityMember>('/community/members/me'),
  listMembers: () => http.get<CommunityMember[]>('/community/members'),
  getMember: (userId: number) => http.get<CommunityMember>(`/community/members/${userId}`),
}
```

- [ ] **Step 2: Build feed page**

`CircleFeedPage` behavior:

- Loads feed with TanStack Query key `['community-feed']`.
- Shows a hero section: `Violet Circle`.
- Shows a primary button linking to `/circle/posts/new`.
- Renders `PostCard` for each post.
- Empty state text: `圈子里还没有动态，发第一条吧。`

- [ ] **Step 3: Build create post page**

`CreatePostPage` behavior:

- Loads topics.
- Uses `PostComposer`.
- On success, invalidates `['community-feed']`.
- Navigates to `/circle/posts/:id`.

- [ ] **Step 4: Build post detail page**

`PostDetailPage` behavior:

- Loads post by route param.
- Loads comments by post id.
- Shows like/unlike button.
- Has comment textarea.
- On comment success, invalidate post and comments queries.
- On like/unlike success, invalidate post and feed queries.

- [ ] **Step 5: Build member pages**

`MembersPage` behavior:

- Loads `/community/members`.
- Shows member cards with display name, username, bio, skills, role/status.

`MemberProfilePage` behavior:

- Loads member details with `communityApi.getMember(Number(id))`.
- Shows display name, username, bio, skills, role, and member status.
- Shows a back link to `/circle/members`.

- [ ] **Step 6: Build simple admin page**

`CommunityAdminPage` behavior:

- Shows copy explaining moderation actions.
- Can be read-only in first frontend pass if admin actions are available only through API tests.
- Hide/restore UI can be added after core pages compile.

- [ ] **Step 7: Update routes**

In `App.tsx`:

```tsx
<Route path="/" element={<Navigate to="/circle" replace />} />
<Route path="/circle" element={<CircleFeedPage />} />
<Route path="/circle/posts/new" element={<CreatePostPage />} />
<Route path="/circle/posts/:id" element={<PostDetailPage />} />
<Route path="/circle/members" element={<MembersPage />} />
<Route path="/circle/members/:id" element={<MemberProfilePage />} />
<Route path="/admin/community" element={<CommunityAdminPage />} />
<Route path="*" element={<Navigate to="/circle" replace />} />
```

Keep existing routes for `/dashboard`, `/project-hub`, `/projects`, `/tasks`, `/notes`, `/daily`, and `/settings/profile`.

- [ ] **Step 8: Update layout navigation**

In `AppLayout.tsx`, add nav items:

```ts
{ key: '/circle', label: '圈子' }
{ key: '/tasks', label: '学习' }
{ key: '/notes', label: '笔记' }
{ key: '/daily', label: '日常' }
{ key: '/circle/members', label: '成员' }
{ key: '/project-hub', label: '项目中心' }
{ key: '/admin/community', label: '管理' }
```

Do not delete existing menu items unless they duplicate these entries.

- [ ] **Step 9: Run frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: TypeScript build and Vite build succeed.

- [ ] **Step 10: Commit Task 6**

Run:

```powershell
git add frontend/src/api/community.ts frontend/src/components/community frontend/src/pages/CircleFeedPage.tsx frontend/src/pages/CreatePostPage.tsx frontend/src/pages/PostDetailPage.tsx frontend/src/pages/MembersPage.tsx frontend/src/pages/MemberProfilePage.tsx frontend/src/pages/CommunityAdminPage.tsx frontend/src/App.tsx frontend/src/layouts/AppLayout.tsx frontend/src/pages/LoginPage.tsx
git commit -m "feat: add violet circle frontend"
```

## Task 7: Documentation, Full Verification, Deploy Notes

**Files:**

- Modify: `README.md`
- Modify: existing API documentation file.
- Modify: existing database documentation file.
- Modify: existing deployment documentation file.

- [ ] **Step 1: Update README**

Add a Violet Circle section with:

- Community feed.
- Posts.
- Comments.
- Likes.
- Topics.
- Members.
- Moderation.
- Existing learning, notes, daily tools remain available.

- [ ] **Step 2: Update API docs**

Document:

```text
GET /api/community/feed
POST /api/community/posts
GET /api/community/posts/{postId}
PUT /api/community/posts/{postId}
DELETE /api/community/posts/{postId}
GET /api/community/posts/{postId}/comments
POST /api/community/posts/{postId}/comments
DELETE /api/community/comments/{commentId}
POST /api/community/posts/{postId}/reactions/like
DELETE /api/community/posts/{postId}/reactions/like
GET /api/community/topics
GET /api/community/members/me
PUT /api/community/members/me/profile
GET /api/community/members
GET /api/community/members/{userId}
POST /api/admin/community/posts/{postId}/hide
POST /api/admin/community/posts/{postId}/restore
POST /api/admin/community/comments/{commentId}/hide
POST /api/admin/community/comments/{commentId}/restore
POST /api/admin/community/members/{userId}/mute
POST /api/admin/community/members/{userId}/unmute
```

- [ ] **Step 3: Update database docs**

Document:

```text
users.role
users.status
circles
circle_members
user_profiles
community_topics
community_posts
community_comments
community_reactions
community_moderation_actions
```

- [ ] **Step 4: Run all backend tests**

Run:

```powershell
cd backend
mvn test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
cd frontend
npm run build
```

Expected: TypeScript and Vite build succeed.

- [ ] **Step 6: Run Docker build check if local Docker is available**

Run:

```powershell
docker compose build
```

Expected: backend and frontend images build.

If Docker is not available on the Windows machine, skip this command and verify Docker build on the server after pushing.

- [ ] **Step 7: Commit Task 7**

Run:

```powershell
git add README.md docs
git commit -m "docs: document violet circle community"
```

## Final Verification Checklist

Run:

```powershell
git status --short
cd backend
mvn test
cd ..\frontend
npm run build
```

Expected:

- Git worktree has no accidental uncommitted changes except files intentionally left for the user.
- Backend tests pass.
- Frontend build passes.

Manual browser checks:

```text
/register -> create user
/login -> login
/circle -> feed loads
/circle/posts/new -> create post
/circle/posts/:id -> view post, comment, like
/circle/members -> member list loads
/tasks -> old learning task page still works
/notes -> old notes page still works
/daily -> old daily page still works
```

## Rollback Plan

If backend migration causes local issues:

1. Stop local containers.
2. Use test profile to reproduce with H2 first.
3. Fix `V6__add_violet_circle_community.sql` before it is deployed.
4. Do not edit an already-applied production migration after production deployment. Add `V7__fix_violet_circle_community.sql` instead.

If frontend routing causes old pages to disappear:

1. Restore old routes in `frontend/src/App.tsx`.
2. Keep `/circle` as an additional route.
3. Rebuild frontend.

## Commit Order

Recommended commits:

```text
feat: add violet circle community foundation
feat: add violet circle posts and feed
feat: add violet circle comments
feat: add violet circle post likes
feat: add violet circle moderation
feat: add violet circle frontend
docs: document violet circle community
```

This commit order keeps each feature reviewable and easy to explain in an interview.
