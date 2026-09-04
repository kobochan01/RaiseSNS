package com.raisesns.backend.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatePostRequestTest {

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

    @Test
    void validRequestHasNoViolations() {
        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(new UpdatePostRequest("更新後の本文"));

        assertThat(violations).isEmpty();
    }

    @Test
    void exactly280CharacterBodyIsAccepted() {
        Set<ConstraintViolation<UpdatePostRequest>> violations =
                validator.validate(new UpdatePostRequest("a".repeat(280)));

        assertThat(violations).isEmpty();
    }

    @Test
    void blankBodyIsRejected() {
        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(new UpdatePostRequest(" "));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void tooLongBodyIsRejected() {
        Set<ConstraintViolation<UpdatePostRequest>> violations =
                validator.validate(new UpdatePostRequest("a".repeat(281)));

        assertThat(violations).isNotEmpty();
    }
}
