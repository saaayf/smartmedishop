package com.smartmedishop.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.UserDto;
import com.smartmedishop.entity.User;
import com.smartmedishop.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> userPage;
            
            if (search != null && !search.trim().isEmpty()) {
                userPage = userService.searchUsers(search, pageable);
            } else {
                userPage = userService.getAllUsers(pageable);
            }
            
            // Convert User entities to UserDto to avoid lazy loading issues
            List<UserDto> userDtos = userPage.getContent().stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", userDtos);
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("size", userPage.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get users: " + e.getMessage()));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData) {
        try {
            User user = new User();
            
            // Set basic fields (required)
            if (userData.containsKey("username") && userData.get("username") != null) {
                user.setUsername(userData.get("username").toString().trim());
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username is required"));
            }
            
            if (userData.containsKey("email") && userData.get("email") != null) {
                user.setEmail(userData.get("email").toString().trim());
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            if (userData.containsKey("password") && userData.get("password") != null) {
                user.setPasswordHash(userData.get("password").toString());
            } else if (userData.containsKey("passwordHash") && userData.get("passwordHash") != null) {
                user.setPasswordHash(userData.get("passwordHash").toString());
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required"));
            }
            
            // Set optional fields
            if (userData.containsKey("firstName") && userData.get("firstName") != null) {
                user.setFirstName(userData.get("firstName").toString().trim());
            }
            if (userData.containsKey("lastName") && userData.get("lastName") != null) {
                user.setLastName(userData.get("lastName").toString().trim());
            }
            if (userData.containsKey("phone") && userData.get("phone") != null) {
                user.setPhone(userData.get("phone").toString().trim());
            }
            if (userData.containsKey("birthDate") && userData.get("birthDate") != null) {
                try {
                    String birthDateStr = userData.get("birthDate").toString();
                    // Parse YYYY-MM-DD format
                    LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    user.setBirthDate(birthDate);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid birth date format. Expected YYYY-MM-DD"));
                }
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Birth date is required"));
            }
            if (userData.containsKey("userType") && userData.get("userType") != null) {
                try {
                    user.setUserType(User.UserType.valueOf(userData.get("userType").toString().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid user type: " + userData.get("userType")));
                }
            }
            
            // Handle isActive and isVerified if provided (otherwise defaults in service)
            if (userData.containsKey("isActive")) {
                user.setIsActive(Boolean.parseBoolean(userData.get("isActive").toString()));
            }
            if (userData.containsKey("isVerified")) {
                user.setIsVerified(Boolean.parseBoolean(userData.get("isVerified").toString()));
            }
            
            User createdUser = userService.createUser(user);
            
            return ResponseEntity.ok(createdUser);
            
        } catch (Exception e) {
            e.printStackTrace(); // Log the full exception
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Unknown error occurred";
            }
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to create user: " + errorMessage));
        }
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert User entity to UserDto to avoid circular reference issues
            UserDto userDto = new UserDto(userOpt.get());
            return ResponseEntity.ok(userDto);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> userData) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // Update user fields
            if (userData.containsKey("firstName")) {
                user.setFirstName(userData.get("firstName").toString());
            }
            if (userData.containsKey("lastName")) {
                user.setLastName(userData.get("lastName").toString());
            }
            if (userData.containsKey("email")) {
                user.setEmail(userData.get("email").toString());
            }
            if (userData.containsKey("phone")) {
                user.setPhone(userData.get("phone").toString());
            }
            if (userData.containsKey("birthDate") && userData.get("birthDate") != null) {
                try {
                    String birthDateStr = userData.get("birthDate").toString();
                    // Parse YYYY-MM-DD format
                    LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    user.setBirthDate(birthDate);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid birth date format. Expected YYYY-MM-DD"));
                }
            }
            if (userData.containsKey("isActive")) {
                user.setIsActive((Boolean) userData.get("isActive"));
            }
            if (userData.containsKey("isVerified")) {
                user.setIsVerified((Boolean) userData.get("isVerified"));
            }
            if (userData.containsKey("userType")) {
                user.setUserType(User.UserType.valueOf(userData.get("userType").toString()));
            }
            if (userData.containsKey("riskProfile")) {
                user.setRiskProfile(User.RiskProfile.valueOf(userData.get("riskProfile").toString()));
            }
            
            User updatedUser = userService.updateUser(user);
            
            return ResponseEntity.ok(updatedUser);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            userService.deleteUser(id);
            
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            user.setIsActive(true);
            userService.updateUser(user);
            
            return ResponseEntity.ok(Map.of("message", "User activated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to activate user: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            user.setIsActive(false);
            userService.updateUser(user);
            
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to deactivate user: " + e.getMessage()));
        }
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FRAUD_ANALYST')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> statistics = userService.getUserStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get user statistics: " + e.getMessage()));
        }
    }
}
