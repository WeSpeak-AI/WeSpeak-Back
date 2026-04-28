package backend.module.user.service;

import backend.core.common.exception.BusinessException;
import backend.core.common.exception.ErrorCode;
import backend.core.domain.user.User;
import backend.module.user.dto.UserProfileResponse;
import backend.core.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = findByEmail(email);
        return UserProfileResponse.from(user);
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
        user.addTicket(1);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
