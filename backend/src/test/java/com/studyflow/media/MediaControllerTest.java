package com.studyflow.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
