package backend.module.auth.service;

import backend.module.auth.domain.Credential;
import backend.module.auth.dto.GoogleLoginRequest;
import backend.module.auth.dto.LoginRequest;
import backend.module.auth.dto.LoginResponse;
import backend.module.auth.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse googleLogin(GoogleLoginRequest request);

    Credential findByEmail(String email);

    Boolean isDuplicate(String email);
}
