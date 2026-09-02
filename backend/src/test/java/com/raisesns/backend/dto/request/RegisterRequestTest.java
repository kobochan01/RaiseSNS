package com.raisesns.backend.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private RegisterRequest valid() {
        return new RegisterRequest("valid_user", "user@example.com", "password123", "表示名");
    }

    @Test
    void validRequestHasNoViolations() {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(valid());

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "invalid name", "invalid-name"})
    void invalidUsernameIsRejected(String username) {
        RegisterRequest request = new RegisterRequest(username, "user@example.com", "password123", "表示名");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void invalidEmailIsRejected() {
        RegisterRequest request = new RegisterRequest("valid_user", "not-an-email", "password123", "表示名");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void tooShortPasswordIsRejected() {
        RegisterRequest request = new RegisterRequest("valid_user", "user@example.com", "short12", "表示名");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void blankDisplayNameIsRejected() {
        RegisterRequest request = new RegisterRequest("valid_user", "user@example.com", "password123", "");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void tooLongDisplayNameIsRejected() {
        RegisterRequest request = new RegisterRequest("valid_user", "user@example.com", "password123", "a".repeat(51));

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
