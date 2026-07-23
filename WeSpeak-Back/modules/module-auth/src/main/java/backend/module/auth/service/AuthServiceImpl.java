package backend.module.auth.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.UserRegisteredEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.core.infra.Snowflake;
import backend.core.jwt.JwtTokenProvider;
import backend.module.auth.domain.Credential;
import backend.module.auth.dto.GoogleLoginRequest;
import backend.module.auth.dto.LoginRequest;
import backend.module.auth.dto.LoginResponse;
import backend.module.auth.dto.RegisterRequest;
import backend.module.auth.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Snowflake snowflake;
    private final RestTemplate restTemplate;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (credentialRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Credential credential = Credential.builder()
                .userId(snowflake.nextId())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Credential.Role.BASIC_USER)
                .build();

        credentialRepository.save(credential);

        publishUserRegistered(credential, request.getNickname());
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

        Credential credential = credentialRepository.findByEmail(email).orElseGet(() -> {
            Credential newCredential = Credential.builder()
                    .userId(snowflake.nextId())
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Credential.Role.BASIC_USER)
                    .build();
            credentialRepository.save(newCredential);
            publishUserRegistered(newCredential, name);
            return newCredential;
        });

        String token = jwtTokenProvider.generateToken(credential.getEmail(), credential.getRole().name());
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(credential.getEmail())
                .nickname(name)
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Credential credential = credentialRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtTokenProvider.generateToken(credential.getEmail(), credential.getRole().name());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(credential.getEmail())
                .build();
    }

    @Override
    public Credential findByEmail(String email) {
        return credentialRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public Boolean isDuplicate(String email) {
        return credentialRepository.existsByEmail(email);
    }

    private void publishUserRegistered(Credential credential, String nickname) {
        outboxEventPublisher.publish(EventType.USER_REGISTERED, UserRegisteredEventPayload.builder()
                .userId(credential.getUserId())
                .email(credential.getEmail())
                .nickname(nickname)
                .registeredAt(LocalDateTime.now())
                .build());
    }
}
