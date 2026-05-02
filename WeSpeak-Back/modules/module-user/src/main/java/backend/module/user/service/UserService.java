package backend.module.user.service;

import backend.module.user.dto.MyPageResponse;
import backend.module.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getProfile(String email);

    MyPageResponse getMyPage(String email);

    void addXp(String email, int amount);

    void rewardTicket(String email);
}
