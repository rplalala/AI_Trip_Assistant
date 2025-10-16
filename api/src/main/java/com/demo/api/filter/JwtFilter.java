package com.demo.api.filter;

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
 * Spring Security JWT 过滤器，从请求头中提取并验证 JWT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtil;
    private final UserRepository userRepository;

    /**
     * 过滤器放行逻辑
     * @param request
     * @param response
     * @param chain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 从请求头 Authorization: Bearer <JWT> 中提取并验证 JWT
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            // 从 Bearer <JWT> 中提取 JWT
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                String subject = claims.getSubject();
                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 确认用户仍存在（避免已删除用户仍可用旧 token）
                    Long userId = Long.valueOf(subject);
                    if (userRepository.existsById(userId)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        subject, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(auth); // 认证成功
                    }
                }
            } catch (RuntimeException e) {
                // 解析失败则不设Authentication，请求到达Controller时会因未认证而被拒绝
                SecurityContextHolder.clearContext();
                log.debug("Invalid token", e);
            }
        }
        chain.doFilter(request, response);
    }
}

