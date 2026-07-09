package com.studyflow.note;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRootNoteReturnsNoteData() throws Exception {
        String token = registerAndLogin("note_alice", "note_alice@example.com");

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "React 学习笔记",
                                  "icon": "book",
                                  "parentId": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value("React 学习笔记"))
                .andExpect(jsonPath("$.data.icon").value("book"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andExpect(jsonPath("$.data.favorite").value(false));
    }

    @Test
    void listNotesOnlyReturnsCurrentUsersNotes() throws Exception {
        String aliceToken = registerAndLogin("note_owner_alice", "note_owner_alice@example.com");
        String bobToken = registerAndLogin("note_owner_bob", "note_owner_bob@example.com");

        createNote(aliceToken, "Alice 的笔记", null);
        createNote(bobToken, "Bob 的笔记", null);

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Alice 的笔记"));
    }

    @Test
    void createChildNoteRequiresOwnedParent() throws Exception {
        String aliceToken = registerAndLogin("note_parent_alice", "note_parent_alice@example.com");
        String bobToken = registerAndLogin("note_parent_bob", "note_parent_bob@example.com");
        Long aliceParentId = createNote(aliceToken, "Alice 父笔记", null);

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "非法子笔记",
                                  "parentId": %d
                                }
                                """.formatted(aliceParentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void saveBlocksReplacesAndReturnsBlocksInOrder() throws Exception {
        String token = registerAndLogin("note_blocks_user", "note_blocks_user@example.com");
        Long noteId = createNote(token, "块编辑器测试", null);

        mockMvc.perform(put("/api/notes/{id}/blocks", noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "type": "heading",
                                    "content": "第一章",
                                    "checked": false,
                                    "sortOrder": 1
                                  },
                                  {
                                    "type": "todo",
                                    "content": "完成后端接口",
                                    "checked": true,
                                    "sortOrder": 2
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.blocks", hasSize(2)))
                .andExpect(jsonPath("$.data.blocks[0].type").value("heading"))
                .andExpect(jsonPath("$.data.blocks[1].checked").value(true));
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
                .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
                .andReturn();

        return loginResult.getResponse()
                .getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private Long createNote(String token, String title, Long parentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "parentId": %s
                                }
                                """.formatted(title, parentId == null ? "null" : parentId.toString())))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");
        return Long.valueOf(id);
    }
}
