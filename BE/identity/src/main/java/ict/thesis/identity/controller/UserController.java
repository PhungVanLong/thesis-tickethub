package ict.thesis.identity.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ict.thesis.identity.dto.UpdateUserRequest;
import ict.thesis.identity.dto.UserResponse;
import ict.thesis.identity.entity.User;
import ict.thesis.identity.repository.UserRepository;
import ict.thesis.identity.service.AuthService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        Optional<User> u = userRepository.findById(id);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setFullName(user.getFullName());
        resp.setRole(user.getRole());
        resp.setVerified(user.isVerified());
        resp.setActive(user.isActive());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @PutMapping
    public ResponseEntity<?> updateUserMissingId(@RequestBody UpdateUserRequest request) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing id in path. Use /api/users/{id}");
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
        @RequestHeader(value = "X-User-Role",required = false) String role){
if  (role== null|| !role.contains("ADMIN")){
    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access Denied"
    );
        }
    List<User> users = userRepository.findAll();
    List<UserResponse> response = users.stream().map(user ->{
        UserResponse resp=new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setFullName(user.getFullName());
        resp.setRole(user.getRole());
        resp.setVerified(user.isVerified());
        resp.setActive(user.isActive());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setUpdatedAt(user.getUpdatedAt());
        return resp;
    }).toList();
    return ResponseEntity.ok(response);
    }
}
