package com.leavemanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * INTERVIEW NOTE:
 * This filter intercepts all incoming HTTP requests to check for a JWT in the "Authorization" header.
 *
 * It extends OncePerRequestFilter to guarantee that it executes exactly ONCE per request thread.
 * This is crucial because some servlet containers can forward requests internally, and we don't
 * want authentication logic running multiple times for the same logical request.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. Retrieve the Authorization header from the request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. If the header is missing or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the actual token (skip the "Bearer " prefix, which is 7 characters)
        jwt = authHeader.substring(7);
        try {
            username = jwtUtil.extractUsername(jwt);
            
            // 4. Verify username is extracted and the user is NOT already authenticated in the current security context.
            // Checking if authentication is null prevents redundant database calls and re-authenticating on the same thread.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 5. Load user details from the database using UserDetailsService
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // 6. Validate the token signature and expiration
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    // 7. Create an authentication token representing the authenticated user.
                    // Principal: userDetails
                    // Credentials: null (since we are authenticated by JWT, not active password)
                    // Authorities: userDetails.getAuthorities() (role claims)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // 8. Associate details about the request (e.g. IP address, session ID) to our auth token
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 9. Store the authentication object in the SecurityContext.
                    // Once set, subsequent Spring filters and controllers consider the request authenticated.
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        // Proceed to the next filter in the security filter chain
        filterChain.doFilter(request, response);
    }
}
