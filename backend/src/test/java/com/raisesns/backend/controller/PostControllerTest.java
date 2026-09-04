package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.request.UpdatePostRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// タイムライン取得はテーブル全体を対象にするため、テストごとに他テストの投稿が混ざらないようトランザクションをロールバックする。
@AutoConfigureMockMvc
@Transactional
class PostControllerTest extends AbstractIntegrationTest {

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
                        .content(objectMapper.writeValueAsString(
                                new com.raisesns.backend.dto.request.LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = extractCookieValue(loginResult.getResponse().getHeaders("Set-Cookie"), "ACCESS_TOKEN");
        return new Cookie("ACCESS_TOKEN", accessToken);
    }

    @Test
    void createReturns201WithAuthorInfo() throws Exception {
        Cookie accessToken = registerAndLogin("poster1", "poster1@example.com");

        mockMvc.perform(post("/api/posts")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("はじめての投稿"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.body").value("はじめての投稿"))
                .andExpect(jsonPath("$.author.username").value("poster1"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0))
                .andExpect(jsonPath("$.isLikedByMe").value(false));
    }

    @Test
    void createReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("投稿"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns400ForBlankBody() throws Exception {
        Cookie accessToken = registerAndLogin("poster2", "poster2@example.com");

        mockMvc.perform(post("/api/posts")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest(" "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturns200WhenRequesterIsOwner() throws Exception {
        Cookie accessToken = registerAndLogin("owner1", "owner1@example.com");
        Long postId = createPost(accessToken, "元の本文");

        mockMvc.perform(put("/api/posts/" + postId)
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("更新後の本文"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("更新後の本文"));
    }

    @Test
    void updateReturns403WhenRequesterIsNotOwner() throws Exception {
        Cookie ownerToken = registerAndLogin("owner2", "owner2@example.com");
        Long postId = createPost(ownerToken, "元の本文");
        Cookie otherToken = registerAndLogin("other2", "other2@example.com");

        mockMvc.perform(put("/api/posts/" + postId)
                        .cookie(otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("乗っ取り"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReturns404WhenPostDoesNotExist() throws Exception {
        Cookie accessToken = registerAndLogin("owner3", "owner3@example.com");

        mockMvc.perform(put("/api/posts/999999")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("更新後の本文"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204WhenRequesterIsOwner() throws Exception {
        Cookie accessToken = registerAndLogin("owner4", "owner4@example.com");
        Long postId = createPost(accessToken, "削除される投稿");

        mockMvc.perform(delete("/api/posts/" + postId).cookie(accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns403WhenRequesterIsNotOwner() throws Exception {
        Cookie ownerToken = registerAndLogin("owner5", "owner5@example.com");
        Long postId = createPost(ownerToken, "削除される投稿");
        Cookie otherToken = registerAndLogin("other5", "other5@example.com");

        mockMvc.perform(delete("/api/posts/" + postId).cookie(otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void timelineReturnsPostsInDescendingOrder() throws Exception {
        Cookie accessToken = registerAndLogin("timelineuser1", "timelineuser1@example.com");
        createPost(accessToken, "1件目");
        createPost(accessToken, "2件目");
        createPost(accessToken, "3件目");

        mockMvc.perform(get("/api/posts").cookie(accessToken).param("scope", "all").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].body").value("3件目"))
                .andExpect(jsonPath("$.posts[1].body").value("2件目"))
                .andExpect(jsonPath("$.posts[2].body").value("1件目"));
    }

    @Test
    void timelinePaginatesWithCursor() throws Exception {
        Cookie accessToken = registerAndLogin("timelineuser2", "timelineuser2@example.com");
        createPost(accessToken, "1件目");
        createPost(accessToken, "2件目");
        createPost(accessToken, "3件目");

        MvcResult firstPage = mockMvc.perform(get("/api/posts")
                        .cookie(accessToken).param("scope", "all").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNumber())
                .andReturn();

        Long nextCursor = objectMapper.readTree(firstPage.getResponse().getContentAsString())
                .get("nextCursor").asLong();

        mockMvc.perform(get("/api/posts")
                        .cookie(accessToken).param("scope", "all").param("limit", "2")
                        .param("cursor", String.valueOf(nextCursor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].body").value("1件目"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void timelineReturnsEmptyListForFollowingScope() throws Exception {
        Cookie accessToken = registerAndLogin("timelineuser3", "timelineuser3@example.com");
        createPost(accessToken, "1件目");

        mockMvc.perform(get("/api/posts").cookie(accessToken).param("scope", "following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void timelineReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());
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
}
