package co.dulcesydulces.provedor_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.dulcesydulces.provedor_backend.domain.dto.LoginRequest;
import co.dulcesydulces.provedor_backend.domain.dto.LoginResponse;
import co.dulcesydulces.provedor_backend.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse res = authService.login(req);

        if (!res.isOk()) {
            return ResponseEntity.status(401).body(res);
        }
        return ResponseEntity.ok(res);
    }
}
