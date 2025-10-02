package com.example.core.controller;

import com.example.core.dto.AuthRequest;
import com.example.core.dto.AuthResponse;
import com.example.core.dto.RegisterRequest;
import com.example.core.mapper.UserMapper;
import com.example.core.model.Role;
import com.example.core.model.User;
import com.example.core.repository.UserRepository;
import com.example.core.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
package com.example.core.controller.auth;

import com.example.core.dto.auth.AuthRequest;
import com.example.core.dto.auth.AuthResponse;
import com.example.core.dto.auth.RegisterRequest;
import com.example.core.dto.user.UserDTO;
import com.example.core.mapper.UserMapper;
import com.example.core.model.Role;
import com.example.core.model.User;
import com.example.core.repository.UserRepository;
import com.example.core.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

        import jakarta.validation.Valid;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        User user = userMapper.fromRegisterRequest(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.CLIENTE);

        userRepository.save(user);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authReq) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authReq.getEmail(), authReq.getPassword())
        );

        var user = userRepository.findByEmail(authReq.getEmail()).orElseThrow();

        var claims = new HashMap<String, Object>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());

        String token = jwtUtil.generateToken(user.getEmail(), claims);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}

}
