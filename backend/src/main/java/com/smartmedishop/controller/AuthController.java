package com.smartmedishop.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.AuthResponse;
import com.smartmedishop.dto.LoginRequest;
import com.smartmedishop.dto.RegisterRequest;
import com.smartmedishop.entity.User;
import com.smartmedishop.security.CustomUserDetailsService;
import com.smartmedishop.security.JwtService;
import com.smartmedishop.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            if (userService.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
            }
            
            // Check if email already exists
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already exists"));
            }
            
            // Create user
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPasswordHash(registerRequest.getPassword());
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setPhone(registerRequest.getPhone());
            user.setBirthDate(registerRequest.getBirthDate());
            
            // Set user type based on role
            String role = registerRequest.getRole() != null ? registerRequest.getRole().toLowerCase() : "user";
            switch (role) {
                case "admin":
                    user.setUserType(User.UserType.ADMIN);
                    break;
                case "fraud_analyst":
                case "fraud analyst":
                    user.setUserType(User.UserType.FRAUD_ANALYST);
                    break;
                case "supplier":
                    user.setUserType(User.UserType.SUPPLIER);
                    break;
                case "nurse":
                    user.setUserType(User.UserType.NURSE);
                    break;
                case "delivery_man":
                case "delivery man":
                case "deliveryman":
                    user.setUserType(User.UserType.DELIVERY_MAN);
                    break;
                case "technical_support":
                case "technical support":
                case "tech_support":
                    user.setUserType(User.UserType.TECHNICAL_SUPPORT);
                    break;
                case "user":
                case "customer":
                default:
                    user.setUserType(User.UserType.CUSTOMER);
                    break;
            }
            
            User savedUser = userService.createUser(user);
            
            // Generate JWT token
            var userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
            String token = jwtService.generateToken(userDetails);
            
            // Create response
            AuthResponse response = new AuthResponse(
                token,
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getUserType().name(),
                savedUser.getId()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get user details
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            // Update last login
            userService.updateLastLogin(user.getId());
            
            // Generate JWT token
            String token = jwtService.generateToken(userDetails);
            
            // Create response
            AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getUserType().name(),
                user.getId()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            var userDetails = (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userDetails.getUser();
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("firstName", user.getFirstName());
            profile.put("lastName", user.getLastName());
            profile.put("phone", user.getPhone());
            profile.put("userType", user.getUserType());
            profile.put("riskProfile", user.getRiskProfile());
            profile.put("registrationDate", user.getRegistrationDate());
            profile.put("lastLogin", user.getLastLogin());
            profile.put("isActive", user.getIsActive());
            profile.put("isVerified", user.getIsVerified());
            
            return ResponseEntity.ok(profile);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get profile: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "SmartMediShop Authentication Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
