package com.raisesns.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisesns.backend.dto.request.LoginRequest;
import com.raisesns.backend.dto.request.RegisterRequest;
import com.raisesns.backend.exception.InvalidImageException;
import com.raisesns.backend.service.ImageStorageService;
import com.raisesns.backend.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ImageControllerTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ImageStorageService imageStorageService;

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
    void uploadPostImageReturns200WithImageUrl() throws Exception {
        Cookie accessToken = registerAndLogin("imageuser1", "imageuser1@example.com");
        when(imageStorageService.uploadPostImage(any()))
                .thenReturn("https://example-bucket.s3.ap-northeast-1.amazonaws.com/posts/a.png");
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/images/posts").file(file).cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://example-bucket.s3.ap-northeast-1.amazonaws.com/posts/a.png"));
    }

    @Test
    void uploadAvatarImageReturns200WithImageUrl() throws Exception {
        Cookie accessToken = registerAndLogin("imageuser2", "imageuser2@example.com");
        when(imageStorageService.uploadAvatarImage(any()))
                .thenReturn("https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png");
        MockMultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/images/avatars").file(file).cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/a.png"));
    }

    @Test
    void uploadPostImageReturns401WhenNotAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/images/posts").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadPostImageReturns400WhenServiceRejectsFile() throws Exception {
        Cookie accessToken = registerAndLogin("imageuser3", "imageuser3@example.com");
        when(imageStorageService.uploadPostImage(any()))
                .thenThrow(new InvalidImageException("only jpg, jpeg, png, gif, webp images are allowed"));
        MockMultipartFile file = new MockMultipartFile("image", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/images/posts").file(file).cookie(accessToken))
                .andExpect(status().isBadRequest());
    }
}
