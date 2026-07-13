package com.studyflow.community;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackgroundPresetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void backgroundPresetsCanBeListedPubliclyByPlacement() throws Exception {
        mockMvc.perform(get("/api/background-presets")
                        .param("placement", "PROFILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].url", hasItem("/system-backgrounds/profile/road.png")))
                .andExpect(jsonPath("$.data[*].url", hasItem("/system-backgrounds/profile/silhouette.mp4")));

        mockMvc.perform(get("/api/background-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].url", hasItem("/system-backgrounds/profile/road.png")));
    }

    @Test
    void homeBackgroundPlacementIsNoLongerSupported() throws Exception {
        mockMvc.perform(get("/api/background-presets")
                        .param("placement", "HOME"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyRuruCanCreateSystemBackgroundPresets() throws Exception {
        String normalToken = registerAndLogin("background_normal", "background_normal@example.com");

        mockMvc.perform(post("/api/admin/background-presets")
                        .header("Authorization", "Bearer " + normalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placement": "PROFILE",
                                  "name": "normal upload",
                                  "url": "/api/media/files/201",
                                  "mediaType": "IMAGE"
                                }
                                """))
                .andExpect(status().isForbidden());

        String ruruToken = ensureRuruAndLogin("background-ruru@example.com");
        mockMvc.perform(post("/api/admin/background-presets")
                        .header("Authorization", "Bearer " + ruruToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placement": "PROFILE",
                                  "name": "shared profile background",
                                  "url": "/api/media/files/202",
                                  "mediaType": "IMAGE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placement").value("PROFILE"))
                .andExpect(jsonPath("$.data.name").value("shared profile background"))
                .andExpect(jsonPath("$.data.url").value("/api/media/files/202"))
                .andExpect(jsonPath("$.data.mediaType").value("IMAGE"));

        mockMvc.perform(get("/api/background-presets")
                        .param("placement", "PROFILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].url", hasItem("/api/media/files/202")));
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
