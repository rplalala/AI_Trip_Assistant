package com.demo.api.filter;

import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import com.demo.api.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("Valid token sets authentication in security context")
    @Test
    void doFilter_setsAuthentication() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        Claims claims = new DefaultClaims();
        claims.setSubject("42");
        claims.put("version", 3);
        when(jwtUtils.parse("token")).thenReturn(claims);

        User user = new User();
        user.setTokenVersion(3);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("42");
        verify(chain).doFilter(request, response);
    }

    @DisplayName("Invalid token clears context and continues chain")
    @Test
    void doFilter_invalidToken() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");

        doThrow(new RuntimeException("bad token")).when(jwtUtils).parse(anyString());

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @DisplayName("Token version mismatch clears context")
    @Test
    void doFilter_versionMismatch() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");

        Claims claims = new DefaultClaims();
        claims.setSubject("7");
        claims.put("version", 2);
        when(jwtUtils.parse("token")).thenReturn(claims);
        User user = new User();
        user.setTokenVersion(1);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @DisplayName("No Authorization header leaves context untouched")
    @Test
    void doFilter_noAuthorizationHeader() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilterInternal(request, response, chain);

        verify(jwtUtils, never()).parse(anyString());
        verify(chain).doFilter(request, response);
    }
}
