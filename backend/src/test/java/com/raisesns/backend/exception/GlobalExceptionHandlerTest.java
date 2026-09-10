package com.raisesns.backend.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.raisesns.backend.dto.response.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logbackLogger;

    @BeforeEach
    void attachAppender() {
        logbackLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    void detachAppender() {
        logbackLogger.detachAppender(listAppender);
    }

    @Test
    void unexpectedExceptionReturns500AndLogsAtErrorWithStackTrace() {
        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("予期しないエラーが発生しました");

        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent event = listAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
    }
}
