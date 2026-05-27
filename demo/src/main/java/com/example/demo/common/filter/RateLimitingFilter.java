package com.example.demo.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter implements Filter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private static final int BUCKET_CAPACITY = 20; 
    private static final int REFILL_TOKENS_PER_SECOND = 2; 

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "rate-limit-cleaner");
        thread.setDaemon(true);
        return thread;
    });

    public RateLimitingFilter() {
        // Run cleanup every 10 minutes to evict buckets inactive for more than 10 minutes
        scheduler.scheduleAtFixedRate(this::cleanupExpiredBuckets, 10, 10, TimeUnit.MINUTES);
    }

    private void cleanupExpiredBuckets() {
        long now = System.nanoTime();
        long expirationThresholdNano = TimeUnit.MINUTES.toNanos(10);
        buckets.entrySet().removeIf(entry -> (now - entry.getValue().getLastAccessTimestamp()) > expirationThresholdNano);
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String requestURI = httpRequest.getRequestURI();
            
            if (requestURI.startsWith("/api/")) {
                String ip = httpRequest.getRemoteAddr();
                TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_TOKENS_PER_SECOND));

                if (!bucket.tryConsume()) {
                    httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    Map<String, Object> errorDetails = Map.of(
                            "success", false,
                            "message", "Too many requests. Rate limit exceeded. Please try again later.",
                            "status", HttpStatus.TOO_MANY_REQUESTS.value()
                    );
                    
                    new ObjectMapper().writeValue(httpResponse.getOutputStream(), errorDetails);
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    private static class TokenBucket {
        private final long capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;
        private long lastAccessTimestamp;

        public TokenBucket(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
            this.lastAccessTimestamp = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            this.lastAccessTimestamp = System.nanoTime();
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public long getLastAccessTimestamp() {
            return lastAccessTimestamp;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedTimeSeconds = (now - lastRefillTimestamp) / 1e9;
            double tokensToAdd = elapsedTimeSeconds * refillRatePerSecond;
            
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }
}
