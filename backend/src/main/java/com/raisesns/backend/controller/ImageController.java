package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.ImageUploadResponse;
import com.raisesns.backend.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@Tag(name = "画像")
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Operation(summary = "投稿画像アップロード")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadPostImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(new ImageUploadResponse(imageStorageService.uploadPostImage(image)));
    }

    @Operation(summary = "プロフィール画像アップロード")
    @PostMapping(value = "/avatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadAvatarImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(new ImageUploadResponse(imageStorageService.uploadAvatarImage(image)));
    }
}
