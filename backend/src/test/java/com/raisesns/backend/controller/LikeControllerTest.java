package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class LikeControllerTest extends AbstractIntegrationTest {

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
    void likeReturns200WithIncrementedCountAndLikedTrue() throws Exception {
        Cookie ownerToken = registerAndLogin("likeowner1", "likeowner1@example.com");
        Long postId = createPost(ownerToken, "いいねされる投稿");
        Cookie likerToken = registerAndLogin("liker1", "liker1@example.com");

        mockMvc.perform(post("/api/posts/" + postId + "/likes").cookie(likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.isLikedByMe").value(true));
    }

    @Test
    void likeTwiceDoesNotDoubleCount() throws Exception {
        Cookie ownerToken = registerAndLogin("likeowner2", "likeowner2@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie likerToken = registerAndLogin("liker2", "liker2@example.com");

        mockMvc.perform(post("/api/posts/" + postId + "/likes").cookie(likerToken)).andExpect(status().isOk());

        mockMvc.perform(post("/api/posts/" + postId + "/likes").cookie(likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void unlikeReturns200WithDecrementedCountAndLikedFalse() throws Exception {
        Cookie ownerToken = registerAndLogin("likeowner3", "likeowner3@example.com");
        Long postId = createPost(ownerToken, "投稿");
        Cookie likerToken = registerAndLogin("liker3", "liker3@example.com");
        mockMvc.perform(post("/api/posts/" + postId + "/likes").cookie(likerToken)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/posts/" + postId + "/likes").cookie(likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.isLikedByMe").value(false));
    }

    @Test
    void likeReturns404WhenPostDoesNotExist() throws Exception {
        Cookie likerToken = registerAndLogin("liker4", "liker4@example.com");

        mockMvc.perform(post("/api/posts/999999/likes").cookie(likerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void likeReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/posts/1/likes"))
                .andExpect(status().isUnauthorized());
    }
}
