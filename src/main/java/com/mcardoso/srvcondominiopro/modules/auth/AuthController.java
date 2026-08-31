package com.mcardoso.srvcondominiopro.modules.auth;

import com.mcardoso.srvcondominiopro.modules.auth.dto.AuthResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.ForgotPasswordRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.LoginRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.MessageResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.RefreshTokenRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.RegisterRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.ResetPasswordRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.UsuarioResponse;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(authService.me(usuarioLogado));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
