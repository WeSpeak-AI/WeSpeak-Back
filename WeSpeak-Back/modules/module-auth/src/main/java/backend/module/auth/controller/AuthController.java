package backend.module.auth.controller;

import backend.core.common.response.ApiResponse;
import backend.module.auth.dto.LoginRequest;
import backend.module.auth.dto.LoginResponse;
import backend.module.auth.dto.RegisterRequest;
import backend.module.auth.dto.UsernameDuplicateCheckRequest;
import backend.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/isDuplicate")
    public ApiResponse<Boolean> isDuplicate(@Valid @RequestBody UsernameDuplicateCheckRequest usernameDuplicateCheckRequest) {
        Boolean exist = authService.isDuplicate(usernameDuplicateCheckRequest.getEmail());
        return ApiResponse.ok(exist);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
