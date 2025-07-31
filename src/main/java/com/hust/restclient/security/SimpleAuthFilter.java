package com.hust.restclient.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hust.restclient.dto.CasUserDetail;
import com.hust.restclient.service.CasRestClient;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SimpleAuthFilter extends OncePerRequestFilter {

    private final CasRestClient casRestClient;

    public SimpleAuthFilter(CasRestClient casRestClient) {
        this.casRestClient = casRestClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Skip filter for login and public endpoints
        if (requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/public/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 1: Check session first (fast)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("authenticated_username");
            String role = (String) session.getAttribute("user_role");
            
            if (username != null && role != null) {
                // Session exists - use it
                setAuthentication(username, role);
                System.out.println("User " + username + " authenticated via SESSION");
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Step 2: No session - check CASTGC cookie and validate with CAS
        String castgc = getCastgcCookie(request);
        if (castgc != null) {
            try {
                // Get service ticket
                String serviceTicket = casRestClient.requestServiceTicket(castgc, "http://localhost:8081", null, null);
                
                if (serviceTicket != null) {
                    // Validate and get user details
                    CasUserDetail userDetail = casRestClient.validateServiceTicket(serviceTicket, "http://localhost:8081");
                    
                    if (userDetail.isSuccess()) {
                        // Create new session
                        HttpSession newSession = request.getSession(true);
                        newSession.setAttribute("authenticated_username", userDetail.getUsername());
                        newSession.setAttribute("user_role", userDetail.getRole());
                        newSession.setMaxInactiveInterval(30 * 60); // 30 minutes
                        
                        setAuthentication(userDetail.getUsername(), userDetail.getRole());
                        System.out.println("User " + userDetail.getUsername() + " authenticated via CAS and session created");
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("CAS validation failed: " + e.getMessage());
            }
        }

        // Step 3: No authentication - return 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Authentication required\"}");
    }

    private String getCastgcCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "CASTGC".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void setAuthentication(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username, 
            null, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
