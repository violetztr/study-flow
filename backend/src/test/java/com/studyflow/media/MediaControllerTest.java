package com.studyflow.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void prepareImageUploadReturnsR2SignedUrlAndMediaRecord() throws Exception {
        String token = registerAndLogin("media_image_alice", "media_image_alice@example.com");

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "cat.png",
                                  "contentType": "image/png",
                                  "fileSize": 2048
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaFileId").isNumber())
                .andExpect(jsonPath("$.data.objectKey", containsString("community/images/")))
                .andExpect(jsonPath("$.data.uploadUrl", containsString("test-account.r2.cloudflarestorage.com")))
                .andExpect(jsonPath("$.data.contentType").value("image/png"));
    }

    @Test
    void prepareUploadRejectsNonImageAndOversizedImage() throws Exception {
        String token = registerAndLogin("media_invalid_alice", "media_invalid_alice@example.com");

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "notes.txt",
                                  "contentType": "text/plain",
                                  "fileSize": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "huge.jpg",
                                  "contentType": "image/jpeg",
                                  "fileSize": 10485761
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void completeUploadMarksOwnPendingMediaAsUploaded() throws Exception {
        String token = registerAndLogin("media_complete_alice", "media_complete_alice@example.com");
        Long mediaFileId = prepareImageUpload(token);

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", mediaFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(mediaFileId))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));
    }

    @Test
    void uploadEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/media/uploads/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "guest.png",
                                  "contentType": "image/png",
                                  "fileSize": 2048
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void prepareVideoUploadCompletesAsPendingReview() throws Exception {
        String token = registerAndLogin("media_video_alice", "media_video_alice@example.com");

        MvcResult prepareResult = mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "walk.mp4",
                                  "contentType": "video/mp4",
                                  "fileSize": 2097152
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaFileId").isNumber())
                .andExpect(jsonPath("$.data.objectKey", containsString("community/pending/videos/")))
                .andExpect(jsonPath("$.data.contentType").value("video/mp4"))
                .andReturn();

        Long mediaFileId = objectMapper.readTree(prepareResult.getResponse().getContentAsByteArray())
                .path("data").path("mediaFileId").asLong();

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", mediaFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileType").value("VIDEO"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.transcodeStatus").value("WAITING"));
    }

    @Test
    void prepareVideoUploadAllowsUpToTwoHundredMb() throws Exception {
        String token = registerAndLogin("media_video_200mb", "media_video_200mb@example.com");

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "long-walk.mp4",
                                  "contentType": "video/mp4",
                                  "fileSize": 209715200
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxSizeBytes").value(209715200));
    }

    @Test
    void prepareVideoUploadRejectsUnsupportedAndOversizedVideos() throws Exception {
        String token = registerAndLogin("media_video_invalid", "media_video_invalid@example.com");

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "clip.mov",
                                  "contentType": "video/quicktime",
                                  "fileSize": 1024
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "huge.mp4",
                                  "contentType": "video/mp4",
                                  "fileSize": 209715201
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void videoSubmissionStaysHiddenWhenOnlyMediaIsApproved() throws Exception {
        String authorToken = registerAndLogin("media_video_author", "media_video_author@example.com");
        Long topicId = firstTopicId(authorToken);
        Long videoFileId = prepareAndCompleteVideoUpload(authorToken);
        Long coverFileId = prepareAndCompleteImageUpload(authorToken);
        Long postId = createPost(authorToken, topicId, videoFileId, coverFileId);

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(postId), empty()));

        mockMvc.perform(post("/api/admin/media/{mediaFileId}/approve", videoFileId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isForbidden());

        String ruruToken = registerAndLogin("ruru", "ruru@example.com");
        mockMvc.perform(get("/api/admin/media/pending")
                        .header("Authorization", "Bearer " + ruruToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(videoFileId), hasSize(1)));

        mockMvc.perform(post("/api/admin/media/{mediaFileId}/approve", videoFileId)
                        .header("Authorization", "Bearer " + ruruToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/community/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(postId), empty()));
    }

    @Test
    void ruruCanRetryFailedVideoTranscode() throws Exception {
        String token = registerAndLogin("media_retry_author", "media_retry_author@example.com");
        Long videoFileId = prepareAndCompleteVideoUpload(token);
        markVideoTranscodeFailed(videoFileId);

        mockMvc.perform(post("/api/admin/media/{mediaFileId}/transcode/retry", videoFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        String ruruToken = ensureRuruAndLogin("ruru-retry@example.com");
        mockMvc.perform(post("/api/admin/media/{mediaFileId}/transcode/retry", videoFileId)
                        .header("Authorization", "Bearer " + ruruToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(videoFileId))
                .andExpect(jsonPath("$.data.transcodeStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.transcodeError").doesNotExist());
    }

    @Test
    void videoPostRequiresCoverImage() throws Exception {
        String token = registerAndLogin("media_video_cover_required", "media_video_cover_required@example.com");
        Long topicId = firstTopicId(token);
        Long videoFileId = prepareAndCompleteVideoUpload(token);

        mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Video without cover",
                                  "content": "Video posts need a cover image.",
                                  "mediaFileIds": [%d]
                                }
                                """.formatted(topicId, videoFileId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void hlsMasterPlaylistIsPublicForReadyApprovedVideo() throws Exception {
        String token = registerAndLogin("media_hls_ready", "media_hls_ready@example.com");
        Long videoFileId = prepareAndCompleteVideoUpload(token);
        markVideoTranscodeReady(videoFileId);

        mockMvc.perform(get("/api/media/videos/{mediaFileId}/hls/master.m3u8", videoFileId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("#EXTM3U");
                    assertThat(body).contains("#EXT-X-STREAM-INF");
                    assertThat(body).contains("RESOLUTION=1280x720");
                    assertThat(body).contains("/api/media/videos/%d/hls/720p/index.m3u8".formatted(videoFileId));
                });
    }

    @Test
    void hlsVariantPlaylistRewritesSegmentsToPublicApiUrls() throws Exception {
        String token = registerAndLogin("media_hls_variant", "media_hls_variant@example.com");
        Long videoFileId = prepareAndCompleteVideoUpload(token);
        markVideoTranscodeReady(videoFileId);

        mockMvc.perform(get("/api/media/videos/{mediaFileId}/hls/720p/index.m3u8", videoFileId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("#EXTM3U");
                    assertThat(body).contains("#EXT-X-TARGETDURATION:6");
                    assertThat(body).contains("#EXTINF:6.000,");
                    assertThat(body).contains("/api/media/videos/%d/hls/720p/segments/0.ts".formatted(videoFileId));
                    assertThat(body).contains("#EXT-X-ENDLIST");
                });
    }

    @Test
    void hlsSegmentRedirectsToShortLivedSignedR2Url() throws Exception {
        String token = registerAndLogin("media_hls_segment", "media_hls_segment@example.com");
        Long videoFileId = prepareAndCompleteVideoUpload(token);
        markVideoTranscodeReady(videoFileId);

        mockMvc.perform(get("/api/media/videos/{mediaFileId}/hls/720p/segments/0.ts", videoFileId))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("test-account.r2.cloudflarestorage.com")))
                .andExpect(header().string("Location", containsString(
                        "community/videos/%d/hls/720p/segment-000.ts".formatted(videoFileId)
                )))
                .andExpect(header().string("Location", containsString("X-Amz-Signature")));
    }

    private Long prepareImageUpload(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/media/uploads/presign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "photo.webp",
                                  "contentType": "image/webp",
                                  "fileSize": 4096
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.path("data").path("mediaFileId").asLong();
    }

    private Long prepareAndCompleteImageUpload(String token) throws Exception {
        Long mediaFileId = prepareImageUpload(token);

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
                                  "filename": "clip.webm",
                                  "contentType": "video/webm",
                                  "fileSize": 4096
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long mediaFileId = objectMapper.readTree(prepareResult.getResponse().getContentAsByteArray())
                .path("data").path("mediaFileId").asLong();

        mockMvc.perform(post("/api/media/uploads/{mediaFileId}/complete", mediaFileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return mediaFileId;
    }

    private void markVideoTranscodeReady(Long videoFileId) {
        jdbcTemplate.update("""
                UPDATE media_files
                SET status = 'APPROVED',
                    transcode_status = 'READY',
                    hls_master_object_key = 'community/videos/%d/hls/master.m3u8'
                WHERE id = ?
                """.formatted(videoFileId), videoFileId);
        jdbcTemplate.update("""
                INSERT INTO media_transcode_variants (
                    media_file_id,
                    quality_label,
                    width,
                    height,
                    bitrate_kbps,
                    playlist_object_key,
                    status
                )
                VALUES (?, '720P', 1280, 720, 2800, 'community/videos/%d/hls/720p/index.m3u8', 'READY')
                """.formatted(videoFileId), videoFileId);
        jdbcTemplate.update("""
                INSERT INTO media_transcode_segments (
                    media_file_id,
                    quality_label,
                    segment_index,
                    duration_seconds,
                    object_key,
                    byte_size
                )
                VALUES (?, '720P', 0, 6.000, 'community/videos/%d/hls/720p/segment-000.ts', 1024)
                """.formatted(videoFileId), videoFileId);
    }

    private void markVideoTranscodeFailed(Long videoFileId) {
        jdbcTemplate.update("""
                UPDATE media_files
                SET status = 'APPROVED',
                    transcode_status = 'FAILED',
                    transcode_error = 'ffmpeg failed',
                    transcode_started_at = CURRENT_TIMESTAMP,
                    transcode_completed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, videoFileId);
    }

    private Long firstTopicId(String token) throws Exception {
        MvcResult topicsResult = mockMvc.perform(get("/api/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(topicsResult.getResponse().getContentAsByteArray());
        return response.path("data").get(0).path("id").asLong();
    }

    private Long createPost(String token, Long topicId, Long mediaFileId, Long coverFileId) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "Video post",
                                  "content": "Video is hidden until ruru approves it.",
                                  "mediaFileIds": [%d],
                                  "videoCoverMediaFileId": %d
                                }
                                """.formatted(topicId, mediaFileId, coverFileId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
    }

    private Long createPost(String token, Long topicId, Long mediaFileId) throws Exception {
        MvcResult postResult = mockMvc.perform(post("/api/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": %d,
                                  "title": "带视频的动态",
                                  "content": "视频审核通过前不会公开展示。",
                                  "mediaFileIds": [%d]
                                }
                                """.formatted(topicId, mediaFileId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(postResult.getResponse().getContentAsByteArray());
        return response.path("data").path("id").asLong();
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

    private String ensureRuruAndLogin(String email) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ruru",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andReturn();
        assertThat(registerResult.getResponse().getStatus()).isIn(200, 400);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ruru",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        return response.path("data").path("token").asText();
    }
}
