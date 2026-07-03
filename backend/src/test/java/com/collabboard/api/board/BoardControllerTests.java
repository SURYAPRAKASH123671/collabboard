package com.collabboard.api.board;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BoardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void boardApisRequireJwtAndCreateBoardWithDefaultLists() throws Exception {
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isForbidden());

        String token = signup("board.owner@example.com");

        mockMvc.perform(get("/api/boards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String createBody = """
                {
                  "name": "Interview Prep"
                }
                """;

        String created = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(blankOrNullString())))
                .andExpect(jsonPath("$.name").value("Interview Prep"))
                .andExpect(jsonPath("$.lists.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String boardId = created.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/boards/" + boardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(boardId))
                .andExpect(jsonPath("$.lists[0].title").value("To do"));

        mockMvc.perform(get("/api/boards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(boardId))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void ownerCanInviteExistingUserToBoard() throws Exception {
        String ownerToken = signup("board.invite.owner@example.com");
        String memberToken = signup("board.invite.member@example.com");

        String created = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shared Board\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String boardId = created.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/boards/" + boardId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/boards/" + boardId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"board.invite.member@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("board.invite.member@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/api/boards/" + boardId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(boardId));
    }

    private String signup(String email) throws Exception {
        String signupBody = """
                {
                  "name": "Board Owner",
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);

        return mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .split("\"token\":\"")[1]
                .split("\"")[0];
    }
}
