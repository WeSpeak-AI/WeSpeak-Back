package backend.module.auth.service;

import backend.core.domain.user.User;
import backend.module.auth.dto.LoginRequest;
import backend.module.auth.dto.LoginResponse;
import backend.module.auth.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    User findByEmail(String email);

    Boolean isDuplicate(String email);
}
