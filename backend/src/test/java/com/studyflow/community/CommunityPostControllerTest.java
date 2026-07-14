package com.studyflow.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.community.post.CommunityPost;
import com.studyflow.community.post.CommunityPostMapper;
import com.studyflow.community.post.dto.CommunityPostResponse;
import com.studyflow.community.member.CircleMember;
import com.studyflow.community.member.CircleMemberMapper;
import com.studyflow.community.topic.CommunityTopicMapper;
import com.studyflow.infrastructure.redis.RedisCacheService;
import com.studyflow.infrastructure.redis.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityPostControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircleMemberMapper circleMemberMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private CommunityTopicMapper communityTopicMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private RedisCacheService redisCacheService;

    @Test
    void createPostReturnsPostAndFeedShowsIt() throws Exception {
        String token = registerAndLogin("post_author_alice", "post_author_alice@example.com");
        Long topicId = firstTopicId(token);

        Long postId = createPost(token, topicId, "第一条社区帖子", "今天开始把想法发到 Ruru 社区。");

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].title").value("第一条社区帖子"))
                .andExpect(jsonPath("$.data[0].commentCount").value(0))
                .andExpect(jsonPath("$.data[0].reactionCount").value(0));
    }

    @Test
    void postCollectionOnlyAppearsWhenAuthorEnablesIt() throws Exception {
        String token = registerAndLogin("post_collection_author", "post_collection_author@example.com");
        Long topicId = firstTopicId(token);
        Long firstPostId = createPostWithCollection(
                token,
                topicId,
                "Apex 第一集",
                "先把第一段练习放进去。",
                "Apex 小课",
                "把 Apex 训练内容整理成一个合集。"
        );
        Long secondPostId = createPostWithCollection(
                token,
                topicId,
                "Apex 第二集",
                "第二段练习继续放在同一个合集。",
                "Apex 小课",
                "把 Apex 训练内容整理成一个合集。"
        );
        Long standalonePostId = createPost(token, topicId, "没有专栏的图文", "这个帖子不应该显示专栏合集。");

        mockMvc.perform(get("/api/community/posts/{id}", firstPostId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collection.title").value("Apex 小课"))
                .andExpect(jsonPath("$.data.collection.description").value("把 Apex 训练内容整理成一个合集。"))
                .andExpect(jsonPath("$.data.collection.items", hasSize(2)))
                .andExpect(jsonPath("$.data.collection.items[*].postId", hasItem(firstPostId.intValue())))
                .andExpect(jsonPath("$.data.collection.items[*].postId", hasItem(secondPostId.intValue())));

        JsonNode standalonePost = getPost(token, standalonePostId);
        assertThat(standalonePost.path("collection").isNull() || standalonePost.path("collection").isMissingNode())
                .isTrue();
    }

    @Test
    void collectionItemsArePagedForLargeCollection() throws Exception {
        String token = registerAndLogin("post_collection_paged_author", "post_collection_paged_author@example.com");
        Long topicId = firstTopicId(token);
        Long firstPostId = createPostWithCollection(
                token,
                topicId,
                "分页专栏第 1 集",
                "先创建出一个需要分页的专栏。",
                "Ruru 分页专栏",
                "用来验证专栏合集不会一次性塞太多内容。"
        );
        Long collectionId = getPost(token, firstPostId).path("collection").path("id").asLong();

        for (int index = 2; index <= 25; index++) {
            mockMvc.perform(post("/api/community/posts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "topicId": %d,
                                      "title": "分页专栏第 %d 集",
                                      "content": "第 %d 集内容。",
                                      "collectionEnabled": true,
                                      "collectionId": %d
                                    }
                                    """.formatted(topicId, index, index, collectionId)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/community/collections/{collectionId}/items", collectionId)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(25))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.items[0].postId").value(firstPostId))
                .andExpect(jsonPath("$.data.items[9].title").value("分页专栏第 10 集"));

        mockMvc.perform(get("/api/community/collections/{collectionId}/items", collectionId)
                        .param("page", "3")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(3))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(25))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.items", hasSize(5)))
                .andExpect(jsonPath("$.data.items[4].title").value("分页专栏第 25 集"));
    }

    @Test
    void authorCanListAndSelectExistingCollectionWhenPublishing() throws Exception {
        String token = registerAndLogin("post_collection_selector", "post_collection_selector@example.com");
        Long topicId = firstTopicId(token);
        Long firstPostId = createPostWithCollection(
                token,
                topicId,
                "第一篇专栏内容",
                "先创建出一个专栏。",
                "我的 Java 复盘",
                "把 Java 学习路径串起来。"
        );
        Long collectionId = getPost(token, firstPostId).path("collection").path("id").asLong();

        mockMvc.perform(get("/api/community/collections/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(collectionId))
                .andExpect(jsonPath("$.data[0].title").value("我的 Java 复盘"));

        MvcResult secondResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "第二篇专栏内容",
                                  "content": "发布时选择已有专栏。",
                                  "collectionEnabled": true,
                                  "collectionId": %d
                                }
                                """.formatted(topicId, collectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collection.id").value(collectionId))
                .andReturn();
        Long secondPostId = objectMapper.readTree(secondResult.getResponse().getContentAsByteArray())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/community/posts/{id}", secondPostId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collection.id").value(collectionId))
                .andExpect(jsonPath("$.data.collection.items", hasSize(2)))
                .andExpect(jsonPath("$.data.collection.items[*].postId", hasItem(firstPostId.intValue())))
                .andExpect(jsonPath("$.data.collection.items[*].postId", hasItem(secondPostId.intValue())));
    }

    @Test
    void authorCanEnableRenameAndDisableCollectionAfterPublishing() throws Exception {
        String token = registerAndLogin("post_collection_editor", "post_collection_editor@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "可整理的图文", "先单独发布，之后再加入专栏。");

        mockMvc.perform(put("/api/community/posts/{id}/collection", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "title": "Ruru 开发日志",
                                  "description": "把 Ruru 社区的开发过程串起来。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collection.title").value("Ruru 开发日志"))
                .andExpect(jsonPath("$.data.collection.description").value("把 Ruru 社区的开发过程串起来。"))
                .andExpect(jsonPath("$.data.collection.items", hasSize(1)))
                .andExpect(jsonPath("$.data.collection.items[0].postId").value(postId));

        mockMvc.perform(put("/api/community/posts/{id}/collection", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "title": "Ruru 真实开发日志",
                                  "description": "重新命名后的专栏简介。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collection.title").value("Ruru 真实开发日志"))
                .andExpect(jsonPath("$.data.collection.description").value("重新命名后的专栏简介。"));

        mockMvc.perform(put("/api/community/posts/{id}/collection", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk());

        JsonNode postWithoutCollection = getPost(token, postId);
        assertThat(postWithoutCollection.path("collection").isNull()
                || postWithoutCollection.path("collection").isMissingNode()).isTrue();
    }

    @Test
    void guestFeedWritesShortRedisCache() throws Exception {
        String token = registerAndLogin("post_feed_cache_alice", "post_feed_cache_alice@example.com");
        Long postId = createPost(token, firstTopicId(token), "Feed cache", "Guest feed should be cached briefly.");
        String feedKey = RedisKeys.feed("all", 0);
        doReturn(Optional.empty()).when(redisCacheService).get(eq(feedKey));

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId));

        verify(redisCacheService).get(eq(feedKey));
        verify(redisCacheService).set(eq(feedKey), anyString(), eq(Duration.ofMinutes(2)));
    }

    @Test
    void cachedDetailKeepsLoggedInViewerStateFresh() throws Exception {
        String authorToken = registerAndLogin("post_detail_cache_author", "post_detail_cache_author@example.com");
        String viewerToken = registerAndLogin("post_detail_cache_viewer", "post_detail_cache_viewer@example.com");
        Long postId = createPost(authorToken, firstTopicId(authorToken), "Live title", "Live content");
        mockMvc.perform(post("/api/community/posts/{postId}/reactions/like", postId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
        String detailKey = RedisKeys.postDetail(postId);
        doReturn(Optional.of(objectMapper.writeValueAsString(cachedGuestPost(postId, "Cached title"))))
                .when(redisCacheService)
                .get(eq(detailKey));

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Cached title"))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.piggedByCurrentUser").value(false))
                .andExpect(jsonPath("$.data.favoritedByCurrentUser").value(false));
    }

    @Test
    void cachedFeedOverlaysHotCountersFromRedis() throws Exception {
        String token = registerAndLogin("post_feed_counter_author", "post_feed_counter_author@example.com");
        Long postId = createPost(token, firstTopicId(token), "Counter cached title", "Cached feed should still show hot counters.");
        String feedKey = RedisKeys.feed("all", 0);
        String counterKey = RedisKeys.postCounter(postId);
        doReturn(Optional.of(objectMapper.writeValueAsString(List.of(cachedGuestPost(postId, "Cached feed title")))))
                .when(redisCacheService)
                .get(eq(feedKey));
        doReturn(Optional.of("""
                {
                  "reactionCount": 7,
                  "pigCount": 2,
                  "favoriteCount": 3,
                  "viewCount": 11
                }
                """))
                .when(redisCacheService)
                .get(eq(counterKey));

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Cached feed title"))
                .andExpect(jsonPath("$.data[0].reactionCount").value(7))
                .andExpect(jsonPath("$.data[0].pigCount").value(2))
                .andExpect(jsonPath("$.data[0].favoriteCount").value(3))
                .andExpect(jsonPath("$.data[0].viewCount").value(11));
    }

    @Test
    void searchPostsFindsTitleTopicAndAuthor() throws Exception {
        String token = registerAndLogin("phase6_author_unique", "phase6_author_unique@example.com");
        Long titlePostId = createPost(token, firstTopicId(token), "phase6_apex_title", "Search should match title.");
        Long authorPostId = createPost(token, firstTopicId(token), "No keyword here", "Search should match author username.");
        Long topicPostId = createPostWithTopicName(token, "phase6top", "Topic search title", "Search should match manual topic.");
        Long unrelatedPostId = createPost(token, firstTopicId(token), "plain title", "plain content");

        mockMvc.perform(get("/api/community/search")
                        .param("keyword", "phase6_apex_title")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(titlePostId.intValue())))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(unrelatedPostId.intValue()))));

        mockMvc.perform(get("/api/community/search")
                        .param("keyword", "phase6_author_unique")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(authorPostId.intValue())));

        mockMvc.perform(get("/api/community/search")
                        .param("keyword", "phase6top")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(topicPostId.intValue())));
    }

    @Test
    void hotRankingUsesWeightedInteractionSignals() throws Exception {
        String token = registerAndLogin("phase6_hot_author", "phase6_hot_author@example.com");
        Long topicId = firstTopicId(token);
        Long hotPostId = createPost(token, topicId, "phase6 hot post", "This one should rank first.");
        Long coldPostId = createPost(token, topicId, "phase6 cold post", "This one is newer but colder.");
        overwriteCounters(hotPostId, 9000, 100, 50, 20, 10);
        overwriteCounters(coldPostId, 1, 0, 0, 0, 0);

        mockMvc.perform(get("/api/community/rankings/hot")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(hotPostId));
    }

    @Test
    void updatingPostEvictsFeedAndDetailCache() throws Exception {
        String token = registerAndLogin("post_cache_update_alice", "post_cache_update_alice@example.com");
        JsonNode topics = topics(token);
        Long postId = createPost(token, topics.get(0).path("id").asLong(), "Before cache update", "Before content.");
        clearInvocations(redisCacheService);

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "After cache update",
                                  "content": "After content."
                                }
                                """.formatted(topics.get(0).path("id").asLong())))
                .andExpect(status().isOk());

        verify(redisCacheService).delete(eq(RedisKeys.feed("all", 0)));
        verify(redisCacheService).delete(eq(RedisKeys.postDetail(postId)));
    }

    @Test
    void feedShowsDanmakuCountForPostMetrics() throws Exception {
        String token = registerAndLogin("post_danmaku_metric_alice", "post_danmaku_metric_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "带弹幕的视频", "首页卡片应该能直接看到弹幕数量。");
        markAsPublishedVideo(postId);
        clearInvocations(redisCacheService);

        mockMvc.perform(post("/api/community/posts/{postId}/danmaku", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "第一条弹幕",
                                  "timeSeconds": 3,
                                  "color": "#ffffff"
                                }
                                """))
                .andExpect(status().isOk());
        verify(redisCacheService).delete(eq(RedisKeys.feed("all", 0)));
        verify(redisCacheService).delete(eq(RedisKeys.postDetail(postId)));

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].danmakuCount").value(1));
    }

    @Test
    void openingDetailDoesNotIncreaseViewCount() throws Exception {
        String token = registerAndLogin("post_view_detail_alice", "post_view_detail_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Detail read only", "Opening detail should not count as playback.");

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(0));

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(0));

        assertThat(communityPostMapper.selectById(postId).getViewCount()).isZero();
    }

    @Test
    void qualifiedVideoPlaybackIncrementsOnceAndAppearsInHistory() throws Exception {
        String token = registerAndLogin("post_view_video_alice", "post_view_video_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Video playback", "A real playback should count once.");
        markAsPublishedVideo(postId);

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 12,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));
        verify(redisCacheService).set(eq(RedisKeys.postCounter(postId)), contains("\"viewCount\":1"), eq(Duration.ofHours(6)));

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 30,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(get("/api/community/views/history/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].post.id").value(postId))
                .andExpect(jsonPath("$.data[0].maxProgressSeconds").value(30))
                .andExpect(jsonPath("$.data[0].durationSeconds").value(60));
    }

    @Test
    void qualifiedVideoPlaybackWritesRedisDedupeKeyBeforeCounting() throws Exception {
        String token = registerAndLogin("post_view_redis_alice", "post_view_redis_alice@example.com");
        Long userId = extractUserId(token);
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Redis playback", "Playback dedupe should use Redis first.");
        markAsPublishedVideo(postId);
        String redisKey = RedisKeys.viewDedupe(postId, "user:" + userId);
        doReturn(Optional.of(true))
                .doReturn(Optional.of(false))
                .when(redisCacheService)
                .setIfAbsent(eq(redisKey), eq("1"), eq(Duration.ofHours(6)));

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 12,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 30,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        verify(redisCacheService, times(2))
                .setIfAbsent(eq(redisKey), eq("1"), eq(Duration.ofHours(6)));
        assertThat(communityPostMapper.selectById(postId).getViewCount()).isEqualTo(1);
    }

    @Test
    void redisDedupeFailureFallsBackToMysqlViewRecord() throws Exception {
        String token = registerAndLogin("post_view_redis_fallback_alice", "post_view_redis_fallback_alice@example.com");
        Long userId = extractUserId(token);
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Redis fallback playback", "MySQL remains the final view record.");
        markAsPublishedVideo(postId);
        String redisKey = RedisKeys.viewDedupe(postId, "user:" + userId);
        doReturn(Optional.empty())
                .when(redisCacheService)
                .setIfAbsent(eq(redisKey), eq("1"), eq(Duration.ofHours(6)));

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 12,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 30,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        verify(redisCacheService, times(2))
                .setIfAbsent(eq(redisKey), eq("1"), eq(Duration.ofHours(6)));
        assertThat(communityPostMapper.selectById(postId).getViewCount()).isEqualTo(1);
    }

    @Test
    void unqualifiedVideoPlaybackDoesNotIncreaseViewCount() throws Exception {
        String token = registerAndLogin("post_view_short_alice", "post_view_short_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Short playback", "Too little progress should not count.");
        markAsPublishedVideo(postId);

        mockMvc.perform(post("/api/community/posts/{id}/views", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playedSeconds": 3,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(0));

        assertThat(communityPostMapper.selectById(postId).getViewCount()).isZero();
    }

    @Test
    void createPostCanAttachUploadedImagesAndFeedShowsMedia() throws Exception {
        String token = registerAndLogin("post_media_alice", "post_media_alice@example.com");
        Long topicId = firstTopicId(token);
        Long mediaFileId = prepareAndCompleteImageUpload(token);

        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "带图片的动态",
                                  "content": "这条动态应该显示一张图片。",
                                  "mediaFileIds": [%d]
                                }
                                """.formatted(topicId, mediaFileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentType").value("ARTICLE"))
                .andExpect(jsonPath("$.data.media[0].id").value(mediaFileId))
                .andExpect(jsonPath("$.data.media[0].fileType").value("IMAGE"))
                .andExpect(jsonPath("$.data.media[0].url", containsString("test-account.r2.cloudflarestorage.com")))
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        Long postId = response.path("data").path("id").asLong();

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].media[0].id").value(mediaFileId));
    }

    @Test
    void videoAttachmentIncludesCorrectMetadataWhenVideoPostIsReady() throws Exception {
        String token = registerAndLogin("post_video_ready_alice", "post_video_ready_alice@example.com");
        Long topicId = firstTopicId(token);
        Long videoMediaFileId = prepareAndCompleteVideoUpload(token);
        Long coverMediaFileId = prepareAndCompleteImageUpload(token);
        Long postId = createVideoPost(token, topicId, videoMediaFileId, coverMediaFileId);
        markVideoPostReady(postId, videoMediaFileId);

        mockMvc.perform(get("/api/community/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media[0].id").value(videoMediaFileId))
                .andExpect(jsonPath("$.data.media[0].fileType").value("VIDEO"))
                .andExpect(jsonPath("$.data.media[0].url", containsString("test-account.r2.cloudflarestorage.com")))
                .andExpect(jsonPath("$.data.media[0].coverUrl", containsString("test-account.r2.cloudflarestorage.com")));
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

    @Test
    void mutedOwnerCannotUpdatePost() throws Exception {
        String token = registerAndLogin("post_muted_update_owner", "post_muted_update_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Before mute", "Owner can write before mute.");
        setDefaultMembershipStatus(extractUserId(token), "MUTED");

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Muted update",
                                  "content": "Muted owners should be read only."
                                }
                                """.formatted(topicId)))
                .andExpect(status().isForbidden());

        CommunityPost post = communityPostMapper.selectById(postId);
        assertThat(post.getTitle()).isEqualTo("Before mute");
        assertThat(post.getContent()).isEqualTo("Owner can write before mute.");
    }

    @Test
    void invalidTopicReturnsClientError() throws Exception {
        String token = registerAndLogin("post_invalid_topic_alice", "post_invalid_topic_alice@example.com");

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": 999999,
                                  "title": "Invalid topic",
                                  "content": "This topic should not exist."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void createPostAcceptsManualTopicNameWithoutTopicId() throws Exception {
        String token = registerAndLogin("post_manual_topic_alice", "post_manual_topic_alice@example.com");

        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicName": "apex",
                                  "title": "Manual topic",
                                  "content": "The topic is typed by the author."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").doesNotExist())
                .andExpect(jsonPath("$.data.topicName").value("apex"))
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        Long postId = response.path("data").path("id").asLong();

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(postId))
                .andExpect(jsonPath("$.data[0].topicName").value("apex"));
    }

    @Test
    void softDeletedPostDisappearsFromFeedAndDetail() throws Exception {
        String token = registerAndLogin("post_delete_alice", "post_delete_alice@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Soon deleted", "This will be soft deleted.");

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(postId.intValue()))));

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void successfulUpdateChangesTitleContentAndTopicWithoutBumpingActivity() throws Exception {
        String token = registerAndLogin("post_update_alice", "post_update_alice@example.com");
        JsonNode topics = topics(token);
        Long firstTopicId = topics.get(0).path("id").asLong();
        Long secondTopicId = topics.get(1).path("id").asLong();
        String secondTopicName = topics.get(1).path("name").asText();
        Long postId = createPost(token, firstTopicId, "Original title", "Original content.");
        JsonNode before = getPost(token, postId);
        String beforeLastActivityAt = before.path("lastActivityAt").asText();

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Updated title",
                                  "content": "Updated content."
                                }
                                """.formatted(secondTopicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"))
                .andExpect(jsonPath("$.data.content").value("Updated content."))
                .andExpect(jsonPath("$.data.topicId").value(secondTopicId))
                .andExpect(jsonPath("$.data.topicName").value(secondTopicName))
                .andExpect(jsonPath("$.data.lastActivityAt").value(beforeLastActivityAt));
    }

    @Test
    void anotherUserCannotDeletePost() throws Exception {
        String aliceToken = registerAndLogin("post_delete_owner_alice", "post_delete_owner_alice@example.com");
        String bobToken = registerAndLogin("post_delete_owner_bob", "post_delete_owner_bob@example.com");
        Long topicId = firstTopicId(aliceToken);
        Long postId = createPost(aliceToken, topicId, "Alice delete protected", "Only Alice can delete.");

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutedOwnerCannotDeletePost() throws Exception {
        String token = registerAndLogin("post_muted_delete_owner", "post_muted_delete_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Delete protected by mute", "Muted owner cannot delete.");
        int beforeCount = communityTopicMapper.selectById(topicId).getPostCount();
        setDefaultMembershipStatus(extractUserId(token), "MUTED");

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(communityPostMapper.selectById(postId).getStatus()).isEqualTo("PUBLISHED");
        assertThat(communityTopicMapper.selectById(topicId).getPostCount()).isEqualTo(beforeCount);
    }

    @Test
    void hiddenPostCannotBeUpdatedByOwnerBackToPublished() throws Exception {
        String token = registerAndLogin("post_hidden_update_owner", "post_hidden_update_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Hidden title", "Hidden content.");
        CommunityPost post = communityPostMapper.selectById(postId);
        post.setStatus("HIDDEN");
        communityPostMapper.updateById(post);

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Should not publish",
                                  "content": "Owner edit must not restore hidden post."
                                }
                                """.formatted(topicId)))
                .andExpect(status().isNotFound());

        CommunityPost after = communityPostMapper.selectById(postId);
        assertThat(after.getStatus()).isEqualTo("HIDDEN");
        assertThat(after.getTitle()).isEqualTo("Hidden title");
    }

    @Test
    void duplicatePostDeleteDoesNotDecrementTopicPostCountTwice() throws Exception {
        String token = registerAndLogin("post_duplicate_delete_owner", "post_duplicate_delete_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "Delete once", "Topic count should only decrement once.");
        int afterCreateCount = communityTopicMapper.selectById(topicId).getPostCount();

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(communityTopicMapper.selectById(topicId).getPostCount()).isEqualTo(afterCreateCount - 1);
    }

    @Test
    void topicsAreOrderedAndPostCountUpdatesAfterCreateAndDelete() throws Exception {
        String token = registerAndLogin("post_topic_count_alice", "post_topic_count_alice@example.com");
        JsonNode initialTopics = topics(token);
        Long firstTopicId = initialTopics.get(0).path("id").asLong();
        int initialCount = initialTopics.get(0).path("postCount").asInt();

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].slug").value("announcements"))
                .andExpect(jsonPath("$.data[1].slug").value("chat"))
                .andExpect(jsonPath("$.data[2].slug").value("help"))
                .andExpect(jsonPath("$.data[3].slug").value("share"));

        Long postId = createPost(token, firstTopicId, "Counted post", "This should increment the topic count.");
        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postCount").value(initialCount + 1));

        mockMvc.perform(delete("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].postCount").value(initialCount));
    }

    @Test
    void validationRejectsBlankTitleBlankContentAndOverlongContent() throws Exception {
        String token = registerAndLogin("post_validation_alice", "post_validation_alice@example.com");
        Long topicId = firstTopicId(token);

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": " ",
                                  "content": "Valid content."
                                }
                                """.formatted(topicId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Valid title",
                                  "content": " "
                                }
                                """.formatted(topicId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("topicId", topicId)
                                .put("title", "Valid title")
                                .put("content", "a".repeat(10001))
                                .toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("topicName", "12345678901")
                                .put("title", "Valid title")
                                .put("content", "Valid content")
                                .toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void readRoutesArePublicForGuestsAndNonMembers() throws Exception {
        String memberToken = registerAndLogin("post_public_read_member", "post_public_read_member@example.com");
        Long topicId = firstTopicId(memberToken);
        Long postId = createPost(memberToken, topicId, "公开可读", "游客和非成员都可以先看社区内容。");
        String nonMemberToken = registerAndLogin("post_public_read_non_member", "post_public_read_non_member@example.com");
        removeDefaultMembership(extractUserId(nonMemberToken));

        mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(postId.intValue())));

        mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(false));

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem(postId.intValue())));

        mockMvc.perform(get("/api/community/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.likedByCurrentUser").value(false));
    }

    @Test
    void writeRoutesStillRequireLogin() throws Exception {
        String token = registerAndLogin("post_public_write_owner", "post_public_write_owner@example.com");
        Long topicId = firstTopicId(token);
        Long postId = createPost(token, topicId, "写操作要登录", "游客不能发帖、改帖、删帖或点赞。");

        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "游客发帖",
                                  "content": "这应该被拦截。"
                                }
                                """.formatted(topicId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/community/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "游客改帖",
                                  "content": "这应该被拦截。"
                                }
                                """.formatted(topicId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/community/posts/{id}", postId))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/posts/{id}/reactions/like", postId))
                .andExpect(status().isForbidden());
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

        JsonNode response = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }

    private Long firstTopicId(String token) throws Exception {
        return topics(token).get(0).path("id").asLong();
    }

    private JsonNode topics(String token) throws Exception {
        MvcResult topicsResult = mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(topicsResult.getResponse().getContentAsByteArray());
        return response.path("data");
    }

    private Long createPost(String token, Long topicId, String title, String content) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "%s",
                                  "content": "%s"
                                }
                                """.formatted(topicId, title, content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private Long createPostWithTopicName(String token, String topicName, String title, String content) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicName": "%s",
                                  "title": "%s",
                                  "content": "%s"
                                }
                                """.formatted(topicName, title, content)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private Long createPostWithCollection(
            String token,
            Long topicId,
            String title,
            String content,
            String collectionTitle,
            String collectionDescription
    ) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "%s",
                                  "content": "%s",
                                  "collectionEnabled": true,
                                  "collectionTitle": "%s",
                                  "collectionDescription": "%s"
                                }
                                """.formatted(topicId, title, content, collectionTitle, collectionDescription)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private void overwriteCounters(
            Long postId,
            int viewCount,
            int reactionCount,
            int commentCount,
            int pigCount,
            int favoriteCount
    ) {
        CommunityPost post = communityPostMapper.selectById(postId);
        post.setViewCount(viewCount);
        post.setReactionCount(reactionCount);
        post.setCommentCount(commentCount);
        post.setPigCount(pigCount);
        post.setFavoriteCount(favoriteCount);
        communityPostMapper.updateById(post);
    }

    private void markAsPublishedVideo(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        post.setContentType("VIDEO");
        post.setStatus("PUBLISHED");
        communityPostMapper.updateById(post);
    }

    private Long prepareAndCompleteImageUpload(String token) throws Exception {
        MvcResult prepareResult = mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "feed-image.png",
                                  "contentType": "image/png",
                                  "fileSize": 2048
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode prepareResponse = objectMapper.readTree(prepareResult.getResponse().getContentAsByteArray());
        Long mediaFileId = prepareResponse.path("data").path("mediaFileId").asLong();

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", mediaFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return mediaFileId;
    }

    private Long prepareAndCompleteVideoUpload(String token) throws Exception {
        MvcResult prepareResult = mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "feed-video.mp4",
                                  "contentType": "video/mp4",
                                  "fileSize": 4096
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode prepareResponse = objectMapper.readTree(prepareResult.getResponse().getContentAsByteArray());
        Long mediaFileId = prepareResponse.path("data").path("mediaFileId").asLong();

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", mediaFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return mediaFileId;
    }

    private Long createVideoPost(String token, Long topicId, Long videoMediaFileId, Long coverMediaFileId) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "HLS video",
                                  "content": "Video should expose HLS playback metadata.",
                                  "mediaFileIds": [%d],
                                  "videoCoverMediaFileId": %d
                                }
                                """.formatted(topicId, videoMediaFileId, coverMediaFileId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private void markVideoPostReady(Long postId, Long videoMediaFileId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        post.setContentType("VIDEO");
        post.setStatus("PUBLISHED");
        communityPostMapper.updateById(post);

        jdbcTemplate.update("""
                UPDATE media_files
                SET status = 'APPROVED'
                WHERE id = ?
                """, videoMediaFileId);
    }

    private JsonNode getPost(String token, Long postId) throws Exception {
        MvcResult postResult = mockMvc.perform(get("/api/community/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data");
    }

    private Long extractUserId(String token) throws Exception {
        String[] parts = token.split("\\.");
        JsonNode payload = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(parts[1]));
        return payload.path("userId").asLong();
    }

    private void removeDefaultMembership(Long userId) {
        circleMemberMapper.delete(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, userId));
    }

    private void setDefaultMembershipStatus(Long userId, String status) {
        CircleMember member = circleMemberMapper.selectOne(new LambdaQueryWrapper<CircleMember>()
                .eq(CircleMember::getUserId, userId));
        member.setStatus(status);
        circleMemberMapper.updateById(member);
    }

    private CommunityPostResponse cachedGuestPost(Long postId, String title) {
        LocalDateTime now = LocalDateTime.now();
        return new CommunityPostResponse(
                postId,
                1L,
                1L,
                "cached-author",
                null,
                null,
                "缓存",
                title,
                "Cached content",
                "ARTICLE",
                "PUBLISHED",
                null,
                null,
                null,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                List.of(),
                null,
                now,
                now,
                now
        );
    }
}
