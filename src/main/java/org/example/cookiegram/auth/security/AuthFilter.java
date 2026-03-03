package org.example.cookiegram.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cookiegram.auth.service.AuthService;
import org.example.cookiegram.auth.exception.UnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER = "AUTH_USER";
    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 1) Allow everything that is NOT under /api
        // This includes: /, /index.html, /css/**, /js/**, etc.
        if (!path.startsWith("/api/")) {
            return true;
        }

        // 2) Public API endpoints
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = request.getHeader("X-Auth-Token");
            if (token == null || token.isBlank()) {
                throw new UnauthorizedException("Missing X-Auth-Token");
            }

            AuthenticatedUser user = authService.requireUserByToken(token.trim());
            request.setAttribute(ATTR_USER, user);

            filterChain.doFilter(request, response);

        } catch (UnauthorizedException ex) {
            // Fix 2 (below) also handled here
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }
}