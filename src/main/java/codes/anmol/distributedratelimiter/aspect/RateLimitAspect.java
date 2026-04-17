package codes.anmol.distributedratelimiter.aspect;

import codes.anmol.distributedratelimiter.annotation.RateLimit;
import codes.anmol.distributedratelimiter.exception.RateLimitExceedException;
import codes.anmol.distributedratelimiter.metrics.MetricsService;
import codes.anmol.distributedratelimiter.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;


@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiter fixedWindowRateLimiter;
    private final RateLimiter tokenBucketRateLimiter;
    private final RateLimiter slidingWindowRateLimiter;
    private final MetricsService metricsService;

    public RateLimitAspect(
            @Qualifier("fixedWindowRateLimiter")
            RateLimiter fixedWindowRateLimiter,

            @Qualifier("tokenBucketRateLimiter")
            RateLimiter tokenBucketRateLimiter,

            @Qualifier("slidingWindowRateLimiter")
            RateLimiter slidingWindowRateLimiter,

            MetricsService metricsService
    ) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
        this.slidingWindowRateLimiter = slidingWindowRateLimiter;
        this.metricsService = metricsService;
    }

    @Around("@annotation(codes.anmol.distributedratelimiter.annotation.RateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sign = (MethodSignature) pjp.getSignature();
        Method method = sign.getMethod();
        RateLimit rateLimitAnnotation = method.getAnnotation(RateLimit.class);

        int limit = rateLimitAnnotation.limit();
        int windowSeconds = rateLimitAnnotation.windowSeconds();
        String algorithm = rateLimitAnnotation.algorithm();
        String keyBy = rateLimitAnnotation.keyBy();

        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String rateLimitKey = buildRateLimitKey(httpRequest, keyBy, method);
        RateLimiter limiter = resolveAlgo(algorithm);

        long start = System.currentTimeMillis();
        boolean allowed = limiter.isAllowed(rateLimitKey, limit, windowSeconds);
        long latencyMs = System.currentTimeMillis() - start;

        metricsService.record(algorithm, allowed, latencyMs, rateLimitKey);

        addRateLimitHeaders(limiter, rateLimitKey, limit, windowSeconds, algorithm);

        if (!allowed) {
            throw new RateLimitExceedException(rateLimitKey, limit, windowSeconds);
        }

        return pjp.proceed();
    }

    private void addRateLimitHeaders(
            RateLimiter limiter,
            String rateLimitKey,
            int limit,
            int windowSeconds,
            String algorithm
    ) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletResponse res = attributes.getResponse();
            if (res == null) return;

            long remaining = limiter.getRemaining(rateLimitKey, limit, windowSeconds);
            res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            res.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            res.setHeader("X-RateLimit-Window", String.valueOf(windowSeconds));
            res.setHeader("X-RateLimit-Algorithm", algorithm);
        } catch (Exception _) {
        }
    }

    private String buildRateLimitKey(HttpServletRequest httpRequest, String keyBy, Method method) {
        String methodIdentifier = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        return switch (keyBy.toUpperCase()) {
            case "USER" -> {
                String username = extractUsername(httpRequest);
                yield "aspect:USER:" + username + ":" + methodIdentifier;
            }
            case "GLOBAL" -> "aspect:GLOBAL:" + methodIdentifier;
            default -> {
                String ip = extractIP(httpRequest);
                yield "aspect:IP:" + ip + ":" + methodIdentifier;
            }
        };
    }

    private String extractUsername(HttpServletRequest httpRequest) {
        String userID = httpRequest.getHeader("X-User-ID");
        if (userID != null && !userID.isBlank()) {
            return userID;
        }
        return extractIP(httpRequest);
    }

    private String extractIP(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException(
                    "@RateLimit can only be used on HTTP request-handling methods"
            );
        }
        return attributes.getRequest();
    }

    private RateLimiter resolveAlgo(String algorithm) {
        return switch (algorithm) {
            case "tokenBucket" -> tokenBucketRateLimiter;
            case "slidingWindow" -> slidingWindowRateLimiter;
            default -> fixedWindowRateLimiter;
        };
    }
}
