package com.ltd.gatewayservice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

// Gioi han 10 request moi giay
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile long currentSecond = System.currentTimeMillis() / 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long nowSecond = System.currentTimeMillis() / 1000;
        if (nowSecond != currentSecond) {
            currentSecond = nowSecond;
            requestCount.set(0);
        }

        if (requestCount.incrementAndGet() > MAX_REQUESTS_PER_SECOND) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Vuot qua gioi han 10 request moi giay\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
