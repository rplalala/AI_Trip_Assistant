package com.demo.api.filter;

import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import com.demo.api.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Spring Security JWT filter that extracts and validates JWT from request headers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtil;
    private final UserRepository userRepository;

    /**
     * Filter pass-through logic
     * @param request
     * @param response
     * @param chain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Extract and validate JWT from Authorization header: Bearer <JWT>
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            // Extract JWT from Bearer <JWT>
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                String subject = claims.getSubject();
                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Long userId = Long.valueOf(subject);
                    Integer tokenVersion = claims.get("version", Integer.class);

                    User user = userRepository.findById(userId).orElse(null);
                    // Check whether token version matches (mismatch after password change)
                    if (user != null && tokenVersion.equals(user.getTokenVersion())) {
                        // Set the subject content (String userId) as the JWT principal
                        // In controllers, you can get the principal from the authenticated user's token via @AuthenticationPrincipal (String userId)
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        subject, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(auth); // Authenticated
                    } else {
                        SecurityContextHolder.clearContext();
                    }
                }
            } catch (RuntimeException e) {
                // If parsing fails, do not set Authentication; the request will be rejected at controller due to unauthenticated
                SecurityContextHolder.clearContext();
                log.debug("Invalid token", e);
            }
        }
        chain.doFilter(request, response);
    }
}
