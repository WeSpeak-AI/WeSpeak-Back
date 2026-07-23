package backend.module.user.service;

import backend.core.common.event.EventType;
import backend.core.common.event.payload.UserProfileUpdatedEventPayload;
import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.common.outboxmessagerelay.pub.OutboxEventPublisher;
import backend.module.user.domain.User;
import backend.module.user.domain.UserStatsSnapshot;
import backend.module.user.dto.EditProfileRequest;
import backend.module.user.dto.MyPageResponse;
import backend.module.user.dto.UserProfileResponse;
import backend.module.user.repository.UserRepository;
import backend.module.user.repository.UserStatsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserStatsSnapshotRepository userStatsSnapshotRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = findByEmail(email);
        return UserProfileResponse.from(user);
    }

    @Override
    public MyPageResponse getMyPage(String email) {
        User user = findByEmail(email);
        UserStatsSnapshot stats = userStatsSnapshotRepository.findByUserId(user.getUserId())
                .orElseGet(() -> UserStatsSnapshot.builder().userId(user.getUserId()).build());
        return MyPageResponse.of(
                user.getNickname(),
                user.getEmail(),
                stats.getVocaBookCount(),
                stats.getEssayCount(),
                stats.getUserBookCount(),
                stats.getConversationCount()
        );
    }

    @Override
    @Transactional
    public void updateProfile(String email, EditProfileRequest request) {
        User user = findByEmail(email);
        user.updateNickname(request.getNickname());

        outboxEventPublisher.publish(EventType.USER_PROFILE_UPDATED, UserProfileUpdatedEventPayload.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void addXp(String email, int amount) {
        User user = findByEmail(email);
        user.addXp(amount);
    }

    @Override
    @Transactional
    public void rewardTicket(String email) {
        User user = findByEmail(email);
        user.addTicket(3);
    }

    @Override
    @Transactional
    public void consumeTicket(String email) {
        User user = findByEmail(email);
        user.consumeTicket();
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
