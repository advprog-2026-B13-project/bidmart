package id.ac.ui.cs.advprog.bidmartcore.infrastructure.adapter.input.rest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/health/status",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (shouldSkip(path) || isEventStream(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        long startTime = System.currentTimeMillis();

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = responseWrapper.getStatus();

            String queryString = request.getQueryString();
            String fullPath = queryString != null ? path + "?" + queryString : path;

            log.atLevel(resolveLogLevel(status))
                    .log("[{}] {} {} {} {}ms {}",
                            traceId,
                            request.getMethod(),
                            fullPath,
                            status,
                            responseWrapper.getContentSize(),
                            duration);

            responseWrapper.copyBodyToResponse();
            MDC.remove("traceId");
        }
    }

    private boolean isEventStream(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/event-stream");
    }

    private boolean shouldSkip(String path) {
        for (String prefix : SKIP_PATHS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private org.slf4j.event.Level resolveLogLevel(int status) {
        if (status >= 500) return org.slf4j.event.Level.ERROR;
        if (status >= 400) return org.slf4j.event.Level.WARN;
        return org.slf4j.event.Level.INFO;
    }
}
