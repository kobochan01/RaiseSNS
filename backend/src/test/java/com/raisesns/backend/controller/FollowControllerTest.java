package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class FollowControllerTest extends AbstractIntegrationTest {

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
    void followReturns200WithIncrementedFollowerCountAndFollowedTrue() throws Exception {
        registerAndLogin("followee1", "followee1@example.com");
        Cookie followerToken = registerAndLogin("follower1", "follower1@example.com");

        mockMvc.perform(post("/api/users/followee1/follow").cookie(followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.isFollowedByMe").value(true));
    }

    @Test
    void followTwiceDoesNotDoubleCount() throws Exception {
        registerAndLogin("followee2", "followee2@example.com");
        Cookie followerToken = registerAndLogin("follower2", "follower2@example.com");

        mockMvc.perform(post("/api/users/followee2/follow").cookie(followerToken)).andExpect(status().isOk());

        mockMvc.perform(post("/api/users/followee2/follow").cookie(followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    void followReturns400WhenFollowingSelf() throws Exception {
        Cookie token = registerAndLogin("selffollower", "selffollower@example.com");

        mockMvc.perform(post("/api/users/selffollower/follow").cookie(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followReturns404WhenTargetDoesNotExist() throws Exception {
        Cookie followerToken = registerAndLogin("follower3", "follower3@example.com");

        mockMvc.perform(post("/api/users/nobody/follow").cookie(followerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void followReturns401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/users/someone/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unfollowReturns200WithDecrementedFollowerCountAndFollowedFalse() throws Exception {
        registerAndLogin("followee4", "followee4@example.com");
        Cookie followerToken = registerAndLogin("follower4", "follower4@example.com");
        mockMvc.perform(post("/api/users/followee4/follow").cookie(followerToken)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/followee4/follow").cookie(followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.isFollowedByMe").value(false));
    }

    @Test
    void getFollowingReturnsFollowedUsers() throws Exception {
        registerAndLogin("followee5", "followee5@example.com");
        Cookie followerToken = registerAndLogin("follower5", "follower5@example.com");
        mockMvc.perform(post("/api/users/followee5/follow").cookie(followerToken)).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/follower5/following").cookie(followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].username").value("followee5"))
                .andExpect(jsonPath("$.users[0].isFollowedByMe").value(true));
    }

    @Test
    void getFollowersReturnsFollowerUsers() throws Exception {
        registerAndLogin("followee6", "followee6@example.com");
        Cookie followerToken = registerAndLogin("follower6", "follower6@example.com");
        mockMvc.perform(post("/api/users/followee6/follow").cookie(followerToken)).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/followee6/followers").cookie(followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.length()").value(1))
                .andExpect(jsonPath("$.users[0].username").value("follower6"));
    }
}
