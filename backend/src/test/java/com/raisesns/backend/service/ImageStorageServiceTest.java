package com.raisesns.backend.service;

import com.raisesns.backend.config.S3Properties;
import com.raisesns.backend.exception.ImageUploadFailedException;
import com.raisesns.backend.exception.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageStorageServiceTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Properties s3Properties = new S3Properties("example-bucket", "ap-northeast-1");
    private final ImageStorageService imageStorageService = new ImageStorageService(s3Client, s3Properties);

    @Test
    void uploadPostImageStoresUnderPostsPrefixAndReturnsPublicUrl() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});

        String imageUrl = imageStorageService.uploadPostImage(file);

        assertThat(imageUrl).startsWith("https://example-bucket.s3.ap-northeast-1.amazonaws.com/posts/");
        assertThat(imageUrl).endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadAvatarImageStoresUnderAvatarsPrefix() {
        MockMultipartFile file = new MockMultipartFile("image", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String imageUrl = imageStorageService.uploadAvatarImage(file);

        assertThat(imageUrl).startsWith("https://example-bucket.s3.ap-northeast-1.amazonaws.com/avatars/");
        assertThat(imageUrl).endsWith(".jpg");
    }

    @Test
    void throwsInvalidImageExceptionWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> imageStorageService.uploadPostImage(file))
                .isInstanceOf(InvalidImageException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void throwsInvalidImageExceptionWhenFileExceedsMaxSize() {
        byte[] tooLarge = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("image", "big.png", "image/png", tooLarge);

        assertThatThrownBy(() -> imageStorageService.uploadPostImage(file))
                .isInstanceOf(InvalidImageException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void throwsInvalidImageExceptionWhenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile("image", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> imageStorageService.uploadPostImage(file))
                .isInstanceOf(InvalidImageException.class);
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void throwsImageUploadFailedExceptionWhenS3PutObjectFails() {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertThatThrownBy(() -> imageStorageService.uploadPostImage(file))
                .isInstanceOf(ImageUploadFailedException.class);
    }
}
