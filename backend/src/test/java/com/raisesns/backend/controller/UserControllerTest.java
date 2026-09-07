package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisesns.backend.dto.request.CreatePostRequest;
import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.dto.request.UpdateProfileRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class UserControllerTest extends AbstractIntegrationTest {

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
    void getProfileReturns200WithProfileData() throws Exception {
        Cookie viewerToken = registerAndLogin("viewer1", "viewer1@example.com");
        registerAndLogin("target1", "target1@example.com");

        mockMvc.perform(get("/api/users/target1").cookie(viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("target1"))
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.isFollowedByMe").value(false));
    }

    @Test
    void getProfileReturns404WhenUserDoesNotExist() throws Exception {
        Cookie viewerToken = registerAndLogin("viewer2", "viewer2@example.com");

        mockMvc.perform(get("/api/users/nobody").cookie(viewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfileReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/someone"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfileReturns200AndUpdatesData() throws Exception {
        Cookie ownerToken = registerAndLogin("owner1", "owner1@example.com");

        mockMvc.perform(put("/api/users/owner1")
                        .cookie(ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("新しい表示名", "新しい自己紹介"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("新しい表示名"))
                .andExpect(jsonPath("$.bio").value("新しい自己紹介"));
    }

    @Test
    void updateProfileReturns200AndUpdatesAvatarUrl() throws Exception {
        Cookie ownerToken = registerAndLogin("owner4", "owner4@example.com");

        mockMvc.perform(put("/api/users/owner4")
                        .cookie(ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("新しい表示名", "新しい自己紹介",
                                "https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png"));
    }

    @Test
    void updateProfileReturns403WhenRequesterIsNotOwner() throws Exception {
        Cookie ownerToken = registerAndLogin("owner2", "owner2@example.com");
        Cookie otherToken = registerAndLogin("other2", "other2@example.com");

        mockMvc.perform(put("/api/users/owner2")
                        .cookie(otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("新しい表示名", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProfileReturns400WhenDisplayNameIsBlank() throws Exception {
        Cookie ownerToken = registerAndLogin("owner3", "owner3@example.com");

        mockMvc.perform(put("/api/users/owner3")
                        .cookie(ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserPostsReturnsOnlyThatUsersPosts() throws Exception {
        Cookie ownerToken = registerAndLogin("postowner1", "postowner1@example.com");
        mockMvc.perform(post("/api/posts")
                        .cookie(ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("自分の投稿"))))
                .andExpect(status().isCreated());
        Cookie otherToken = registerAndLogin("otherposter1", "otherposter1@example.com");
        mockMvc.perform(post("/api/posts")
                        .cookie(otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("他人の投稿"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/postowner1/posts").cookie(ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.posts[0].body").value("自分の投稿"));
    }

    @Test
    void searchUsersReturns200WithCaseInsensitivePartialMatches() throws Exception {
        Cookie viewerToken = registerAndLogin("searcher1", "searcher1@example.com");
        registerAndLogin("kobochanTaro", "kobochantaro@example.com");
        registerAndLogin("unrelated1", "unrelated1@example.com");

        mockMvc.perform(get("/api/users/search").cookie(viewerToken).param("keyword", "KOBOCHAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].username").value("kobochanTaro"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void searchUsersReturns400WhenKeywordIsBlank() throws Exception {
        Cookie viewerToken = registerAndLogin("searcher2", "searcher2@example.com");

        mockMvc.perform(get("/api/users/search").cookie(viewerToken).param("keyword", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsersReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/search").param("keyword", "taro"))
                .andExpect(status().isUnauthorized());
    }
}
