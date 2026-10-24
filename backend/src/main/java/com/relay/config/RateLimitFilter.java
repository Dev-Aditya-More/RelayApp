package com.relay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Per-IP request limits, strictest on the sign-in, sign-up and email endpoints. Runs before security. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {
    private record Rule(int max, Duration window) {}

    private static final Map<String, Rule> AUTH_RULES = Map.of(
            "/api/auth/login", new Rule(20, Duration.ofMinutes(1)),
            "/api/auth/register", new Rule(10, Duration.ofHours(1)),
            "/api/auth/forgot-password", new Rule(5, Duration.ofMinutes(15)),
            "/api/auth/reset-password", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/verify-email", new Rule(20, Duration.ofMinutes(15)),
            "/api/auth/resend-verification", new Rule(5, Duration.ofMinutes(15)));
    private static final Rule GENERAL = new Rule(600, Duration.ofMinutes(1));

    private final RateLimiter limiter;
    private final boolean enabled;

    public RateLimitFilter(RateLimiter limiter, @Value("${relay.rate-limit.enabled}") boolean enabled) {
        this.limiter = limiter;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        return !enabled
                || !path.startsWith("/api/")
                || "OPTIONS".equals(req.getMethod())
                || path.equals("/api/health")
                || path.equals("/api/payments/webhook"); // Razorpay must never be throttled
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        String ip = clientIp(req);
        Rule rule = "POST".equals(req.getMethod()) ? AUTH_RULES.get(path) : null;
        long wait = rule != null
                ? limiter.hit(path + "|" + ip, rule.max(), rule.window())
                : limiter.hit("api|" + ip, GENERAL.max(), GENERAL.window());
        if (wait > 0) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(wait));
            res.setContentType("application/json");
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.getWriter().write("{\"message\":\"Too many requests. Please try again in " + RateLimiter.humanWait(wait) + ".\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    /**
     * Leftmost X-Forwarded-For entry when present. Vercel overwrites that header with the real client IP before
     * proxying to the backend, so clients cannot spoof it on the deployed setup.
     */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
