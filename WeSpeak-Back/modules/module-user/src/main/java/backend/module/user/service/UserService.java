package backend.module.user.service;

import backend.module.user.dto.EditProfileRequest;
import backend.module.user.dto.MyPageResponse;
import backend.module.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile(String email);

    void updateProfile(String email, EditProfileRequest request);

    MyPageResponse getMyPage(String email);

    void addXp(String email, int amount);

    void rewardTicket(String email);

    void consumeTicket(String email);
}
