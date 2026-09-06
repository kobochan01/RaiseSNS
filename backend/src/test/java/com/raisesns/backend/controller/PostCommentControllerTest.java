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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PostCommentControllerTest extends AbstractIntegrationTest {

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
    void createReturns201WithAuthorInfo() throws Exception {
        Cookie ownerToken = registerAndLogin("commentowner1", "commentowner1@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie commenterToken = registerAndLogin("commenter1", "commenter1@example.com");

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .cookie(commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("いいですね", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.body").value("いいですね"))
                .andExpect(jsonPath("$.author.username").value("commenter1"))
                .andExpect(jsonPath("$.parentCommentId").doesNotExist())
                .andExpect(jsonPath("$.replies").isArray());
    }

    @Test
    void createReturns404WhenPostDoesNotExist() throws Exception {
        Cookie commenterToken = registerAndLogin("commenter2", "commenter2@example.com");

        mockMvc.perform(post("/api/posts/999999/comments")
                        .cookie(commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("本文", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns400ForBlankBody() throws Exception {
        Cookie ownerToken = registerAndLogin("commentowner2", "commentowner2@example.com");
        Long postId = createPost(ownerToken, "投稿");

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .cookie(ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(" ", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns401WhenNotAuthenticated() throws Exception {
        Cookie ownerToken = registerAndLogin("commentowner3", "commentowner3@example.com");
        Long postId = createPost(ownerToken, "投稿");

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("本文", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCommentsReturnsNestedReplies() throws Exception {
        Cookie ownerToken = registerAndLogin("commentowner4", "commentowner4@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie commenterToken = registerAndLogin("commenter4", "commenter4@example.com");
        Long parentId = createComment(commenterToken, postId, "親コメント", null);
        createComment(ownerToken, postId, "返信コメント", parentId);

        mockMvc.perform(get("/api/posts/" + postId + "/comments").cookie(ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("親コメント"))
                .andExpect(jsonPath("$[0].replies.length()").value(1))
                .andExpect(jsonPath("$[0].replies[0].body").value("返信コメント"))
                .andExpect(jsonPath("$[0].replies[0].parentCommentId").value(parentId));
    }

    @Test
    void getCommentsReturns404WhenPostDoesNotExist() throws Exception {
        Cookie accessToken = registerAndLogin("commenter5", "commenter5@example.com");

        mockMvc.perform(get("/api/posts/999999/comments").cookie(accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCommentsReturns401WhenNotAuthenticated() throws Exception {
        Cookie ownerToken = registerAndLogin("commentowner5", "commentowner5@example.com");
        Long postId = createPost(ownerToken, "投稿");

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isUnauthorized());
    }
}
