package com.posgateway.aml.config;

import com.posgateway.aml.dto.psp.ApiUsageEvent;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.service.psp.ApiUsageTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Intercepts every API request under /api/v1/, maps it to a service type,
 * and fires an async usage-log write to the database so PSP consumption
 * reporting and invoice generation have real data.
 *
 * <p>Only billable endpoints are tracked. Health, actuator, Swagger, and
 * static-resource paths are excluded. The filter never blocks or delays
 * the request — {@link ApiUsageTrackingService#logRequest} is {@code @Async}.
 */
@Component
@Order(2)
public class UsageTrackingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UsageTrackingFilter.class);

    /** URL prefix that the filter watches. */
    private static final String BASE_PREFIX = "/api/v1";

    /** URL-to-service-type mapping. First match wins; compiled at init for speed. */
    private static final Map<Pattern, String> URL_SERVICE_MAP = new ConcurrentHashMap<>();

    static {
        // Transaction processing — the primary revenue driver
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/transactions/ingest.*"), "TRANSACTION_PROCESSING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/sanctions/screen.*"), "SANCTIONS_SCREENING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/screening/.*"), "SCREENING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/aml/check.*"), "AML_CHECK");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/risk-assessment/assess.*"), "RISK_ASSESSMENT");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/reports/(generate|preview|chart).*"), "REPORT_GENERATION");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/cases/.*"), "CASE_MANAGEMENT");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/alerts/.*"), "ALERT_MANAGEMENT");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/merchants/onboard.*"), "MERCHANT_ONBOARDING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/merchants$"), "MERCHANT_MANAGEMENT");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/compliance/sar.*"), "SAR_FILING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/compliance/cbk.*"), "CBK_REPORTING");
        URL_SERVICE_MAP.put(Pattern.compile("^/api/v1/billing/.*"), "BILLING_OPERATIONS");
    }

    /** Paths we never track (health checks, docs, static assets). */
    private static final Pattern[] EXCLUDE_PATTERNS = {
            Pattern.compile("^/api/v1/(auth/login|auth/register|health|swagger|v3/api-docs).*"),
            Pattern.compile("^/actuator/.*"),
            Pattern.compile("^/error$"),
    };

    private final ApiUsageTrackingService trackingService;

    public UsageTrackingFilter(ApiUsageTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(BASE_PREFIX)) return true;
        for (Pattern p : EXCLUDE_PATTERNS) {
            if (p.matcher(path).matches()) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Wrap the request/response so we can read the body (consumed once)
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper resp = new ContentCachingResponseWrapper(response);

        long startNs = System.nanoTime();

        try {
            chain.doFilter(req, resp);
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            resolveAndLog(req, resp, elapsedMs);
            // Ensure the cached response body is written back to the real output stream
            resp.copyBodyToResponse();
        }
    }

    private void resolveAndLog(ContentCachingRequestWrapper req,
                                ContentCachingResponseWrapper resp,
                                long elapsedMs) {
        try {
            String path = req.getRequestURI();
            String method = req.getMethod();
            int status = resp.getStatus();

            // Determine service type from path
            String serviceType = resolveServiceType(path);
            if (serviceType == null) return; // unmapped path — not a billable service

            // PSP ID from security context
            Long pspId = null;
            Long userId = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof User user) {
                pspId = user.getPsp() != null ? user.getPsp().getPspId() : null;
                userId = user.getId();
            }
            if (pspId == null) return; // no PSP context — nothing to bill

            ApiUsageEvent event = ApiUsageEvent.builder()
                    .pspId(pspId)
                    .userId(userId)
                    .endpoint(path)
                    .httpMethod(method)
                    .responseStatus(status)
                    .responseTimeMs((int) Math.min(elapsedMs, Integer.MAX_VALUE))
                    .serviceType(serviceType)
                    .requestId(UUID.randomUUID().toString().substring(0, 12))
                    .timestamp(LocalDateTime.now())
                    .build();

            trackingService.logRequest(event);

        } catch (Exception e) {
            // Usage tracking must never fail the request.
            log.debug("Usage tracking skipped for {}: {}", req.getRequestURI(), e.getMessage());
        }
    }

    static String resolveServiceType(String path) {
        for (Map.Entry<Pattern, String> entry : URL_SERVICE_MAP.entrySet()) {
            if (entry.getKey().matcher(path).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }
}