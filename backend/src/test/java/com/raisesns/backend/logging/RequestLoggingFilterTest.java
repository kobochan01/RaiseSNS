package com.raisesns.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void setsTraceIdInMdcDuringChainEchoesHeaderAndClearsMdcAfter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/posts");
        when(response.getStatus()).thenReturn(200);

        final String[] traceIdDuringChain = new String[1];
        doAnswer(invocation -> {
            traceIdDuringChain[0] = MDC.get("traceId");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(traceIdDuringChain[0]).isNotBlank();
        verify(response).setHeader(eq("X-Request-Id"), eq(traceIdDuringChain[0]));
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void clearsMdcEvenWhenChainThrows() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/posts");
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        assertThat(MDC.get("traceId")).isNull();
    }
}
