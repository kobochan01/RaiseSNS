package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisesns.backend.dto.request.CreateCommentRequest;
import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class CommentControllerTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private Cookie registerAndLogin(String username, String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(username, email, "password123", username);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = extractCookieValue(loginResult.getResponse().getHeaders("Set-Cookie"), "ACCESS_TOKEN");
        return new Cookie("ACCESS_TOKEN", accessToken);
    }

    private Long createPost(Cookie accessToken, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest(body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createComment(Cookie accessToken, Long postId, String body, Long parentCommentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(body, parentCommentId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String extractCookieValue(List<String> setCookieHeaders, String name) {
        for (String header : setCookieHeaders) {
            if (header.startsWith(name + "=")) {
                String rest = header.substring((name + "=").length());
                int semicolon = rest.indexOf(';');
                return semicolon >= 0 ? rest.substring(0, semicolon) : rest;
            }
        }
        throw new IllegalStateException("Cookie not found: " + name);
    }

    @Test
    void deleteReturns200WithDecrementedCommentCount() throws Exception {
        Cookie ownerToken = registerAndLogin("delowner1", "delowner1@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie commenterToken = registerAndLogin("delcommenter1", "delcommenter1@example.com");
        createComment(commenterToken, postId, "1件目");
        Long secondCommentId = createComment(commenterToken, postId, "2件目", null);

        mockMvc.perform(delete("/api/comments/" + secondCommentId).cookie(commenterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1));
    }

    @Test
    void deletingParentCommentCascadesRepliesAndReturnsFinalCount() throws Exception {
        Cookie ownerToken = registerAndLogin("delowner2", "delowner2@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie commenterToken = registerAndLogin("delcommenter2", "delcommenter2@example.com");
        Long parentId = createComment(commenterToken, postId, "親コメント", null);
        createComment(ownerToken, postId, "返信コメント", parentId);

        mockMvc.perform(delete("/api/comments/" + parentId).cookie(commenterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(0));

        mockMvc.perform(get("/api/posts/" + postId + "/comments").cookie(ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteReturns403WhenRequesterIsNotOwner() throws Exception {
        Cookie ownerToken = registerAndLogin("delowner3", "delowner3@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie commenterToken = registerAndLogin("delcommenter3", "delcommenter3@example.com");
        Long commentId = createComment(commenterToken, postId, "コメント", null);

        mockMvc.perform(delete("/api/comments/" + commentId).cookie(ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReturns404WhenCommentDoesNotExist() throws Exception {
        Cookie accessToken = registerAndLogin("delcommenter4", "delcommenter4@example.com");

        mockMvc.perform(delete("/api/comments/999999").cookie(accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isUnauthorized());
    }

    private Long createComment(Cookie accessToken, Long postId, String body) throws Exception {
        return createComment(accessToken, postId, body, null);
    }
}
