package backend.module.user.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.user.User;
import backend.module.user.dto.EditProfileRequest;
import backend.module.user.dto.MyPageResponse;
import backend.module.user.dto.UserProfileResponse;
import backend.core.infra.repository.ConversationStatsRepository;
import backend.core.infra.repository.EssayStatsRepository;
import backend.core.infra.repository.UserBookStatsRepository;
import backend.core.infra.repository.UserRepository;
import backend.core.infra.repository.UserVocaBookStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EssayStatsRepository essayStatsRepository;
    private final UserBookStatsRepository userBookStatsRepository;
    private final UserVocaBookStatsRepository userVocaBookStatsRepository;
    private final ConversationStatsRepository conversationStatsRepository;

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = findByEmail(email);
        return UserProfileResponse.from(user);
    }

    @Override
    public MyPageResponse getMyPage(String email) {
        User user = findByEmail(email);
        return MyPageResponse.of(
                user.getNickname(),
                user.getEmail(),
                userVocaBookStatsRepository.countByUserEmail(email),
                essayStatsRepository.countByUserEmail(email),
                userBookStatsRepository.countByUserEmail(email),
                conversationStatsRepository.countByUserEmail(email)
        );
    }

    @Override
    @Transactional
    public void updateProfile(String email, EditProfileRequest request) {
        User user = findByEmail(email);
        user.updateNickname(request.getNickname());
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

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
