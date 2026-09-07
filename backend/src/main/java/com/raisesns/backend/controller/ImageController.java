package com.raisesns.backend.controller;

import com.raisesns.backend.dto.response.ImageUploadResponse;
import com.raisesns.backend.service.ImageStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/posts")
    public ResponseEntity<ImageUploadResponse> uploadPostImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(new ImageUploadResponse(imageStorageService.uploadPostImage(image)));
    }

    @PostMapping("/avatars")
    public ResponseEntity<ImageUploadResponse> uploadAvatarImage(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(new ImageUploadResponse(imageStorageService.uploadAvatarImage(image)));
    }
}
