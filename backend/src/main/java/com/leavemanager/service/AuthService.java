package com.leavemanager.service;

import com.leavemanager.dto.AuthResponse;
import com.leavemanager.dto.LoginRequest;
import com.leavemanager.dto.RegisterRequest;
import com.leavemanager.entity.Role;
import com.leavemanager.entity.User;
import com.leavemanager.repository.UserRepository;
import com.leavemanager.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * INTERVIEW NOTE:
 * This service handles authentication operations: registration (saving users with hashed passwords),
 * logging in (verifying passwords via AuthenticationManager and issuing JWTs), and retrieving
 * the currently authenticated user details from the security context.
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Registers a new user account with default role EMPLOYEE.
     * Uses Transactional to ensure that if user saving fails, no changes commit to the DB.
     */
    @Transactional
    public User register(RegisterRequest request) {
        // Enforce uniqueness constraints before saving
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create the user object.
        // Lombok's Builder pattern is used here to avoid long constructors and make code readable.
        User user = User.builder()
                .username(request.getUsername())
                // Passwords MUST be hashed using BCrypt before storing in DB (never store plain text)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(Role.EMPLOYEE) // Defaults to EMPLOYEE role upon self-registration
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Authenticates login credentials and generates a JWT.
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate credentials. The authenticationManager compares raw password with stored BCrypt hash.
        // Throws AuthenticationException if verification fails.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Fetch the user profile.
        // We support logging in with either username OR email.
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(request.getUsername());
        }
        
        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 3. Generate a signed JWT token containing user details & roles
        String token = jwtUtil.generateToken(user);
        
        // 4. Return token and basic metadata to client
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    /**
     * Utility method to get the currently logged-in user details from Spring Security context.
     * Helpful in any service layer method where we need to know who is executing the action.
     */
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
