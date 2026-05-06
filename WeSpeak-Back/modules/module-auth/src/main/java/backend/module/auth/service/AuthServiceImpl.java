package backend.module.auth.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.customvoca.CustomVocaBook;
import backend.core.domain.user.User;
import backend.core.infra.Snowflake;
import backend.core.infra.repository.CustomVocaBookRepository;
import backend.core.jwt.JwtTokenProvider;
import backend.module.auth.dto.GoogleLoginRequest;
import backend.module.auth.dto.LoginRequest;
import backend.module.auth.dto.LoginResponse;
import backend.module.auth.dto.RegisterRequest;
import backend.core.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomVocaBookRepository customVocaBookRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Snowflake snowflake;
    private final RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .userId(snowflake.nextId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(User.Role.BASIC_USER)
                .xp(0)
                .streak(0)
                .build();

        userRepository.save(user);

        customVocaBookRepository.save(CustomVocaBook.builder()
                .customVocaBookId(snowflake.nextId())
                .user(user)
                .build());
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken();
        Map<String, String> tokenInfo;
        try {
            tokenInfo = restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        if (tokenInfo == null || tokenInfo.get("email") == null) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        String aud = tokenInfo.get("aud");
        if (!googleClientId.equals(aud)) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        String email = tokenInfo.get("email");
        String name = tokenInfo.getOrDefault("name", email.split("@")[0]);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .userId(snowflake.nextId())
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .nickname(name)
                    .role(User.Role.BASIC_USER)
                    .xp(0)
                    .streak(0)
                    .build();
            userRepository.save(newUser);
            customVocaBookRepository.save(CustomVocaBook.builder()
                    .customVocaBookId(snowflake.nextId())
                    .user(newUser)
                    .build());
            return newUser;
        });

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public Boolean isDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
