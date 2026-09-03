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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerReturns201WithUserInfoAndWithoutPasswordHash() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser1", "newuser1@example.com", "password123", "新規ユーザー");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("newuser1"))
                .andExpect(jsonPath("$.displayName").value("新規ユーザー"))
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerReturns400ForInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("someuser", "not-an-email", "password123", "表示名");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns409ForDuplicateUsername() throws Exception {
        RegisterRequest first = new RegisterRequest("dupuser", "dup1@example.com", "password123", "表示名1");
        RegisterRequest second = new RegisterRequest("dupuser", "dup2@example.com", "password123", "表示名2");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturns200WithHttpOnlyCookieAndUserInfo() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("loginuser", "loginuser@example.com", "password123", "ログインユーザー");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("loginuser@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.displayName").value("ログインユーザー"))
                .andExpect(header().string("Set-Cookie", containsString("ACCESS_TOKEN=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    void loginReturns401ForWrongPassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("wrongpw", "wrongpw@example.com", "password123", "表示名");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("wrongpw@example.com", "incorrect-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("メールアドレスまたはパスワードが正しくありません"));
    }

    @Test
    void loginReturns401ForNonExistentEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nobody@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("メールアドレスまたはパスワードが正しくありません"));
    }

    @Test
    void loginSetsAccessAndRefreshTokenCookies() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("cookieuser", "cookieuser@example.com", "password123", "クッキーユーザー");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("cookieuser@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("REFRESH_TOKEN="))));
    }

    @Test
    void refreshIssuesNewAccessTokenAndUserInfo() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("refreshuser", "refreshuser@example.com", "password123", "リフレッシュユーザー");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("refreshuser@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshTokenValue = extractCookieValue(
                loginResult.getResponse().getHeaders("Set-Cookie"), "REFRESH_TOKEN");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", refreshTokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("リフレッシュユーザー"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("REFRESH_TOKEN="))));
    }

    @Test
    void refreshReturns401ForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", "invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenSoSubsequentRefreshFails() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("logoutuser", "logoutuser@example.com", "password123", "ログアウトユーザー");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("logoutuser@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshTokenValue = extractCookieValue(
                loginResult.getResponse().getHeaders("Set-Cookie"), "REFRESH_TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("REFRESH_TOKEN", refreshTokenValue)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", refreshTokenValue)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExpiresBothCookies() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(allOf(containsString("ACCESS_TOKEN="), containsString("Max-Age=0")))))
                .andExpect(header().stringValues("Set-Cookie",
                        hasItem(allOf(containsString("REFRESH_TOKEN="), containsString("Max-Age=0")))));
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
