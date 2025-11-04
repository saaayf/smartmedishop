package com.smartmedishop.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmedishop.dto.UserDto;
import com.smartmedishop.entity.User;
import com.smartmedishop.service.UserService;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> testUsers() {
        try {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
            Page<User> userPage = userService.getAllUsers(pageable);
            
            // Convert User entities to UserDto to avoid lazy loading issues
            List<UserDto> userDtos = userPage.getContent().stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", userDtos);
            response.put("total", userPage.getTotalElements());
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> testStatistics() {
        try {
            Map<String, Object> stats = userService.getUserStatistics();
            stats.put("status", "success");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.ok(response);
        }
    }
}