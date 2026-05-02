package backend.module.user.controller;

import backend.core.common.response.ApiResponse;
import backend.module.user.dto.MyPageResponse;
import backend.module.user.dto.UserProfileResponse;
import backend.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Gateway에서 JWT 검증 후 X-Username 헤더(email)로 유저 식별
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(userService.getProfile(email));
    }

    @GetMapping("/mypage")
    public ApiResponse<MyPageResponse> getMyPage(
            @RequestHeader("X-Username") String email
    ) {
        return ApiResponse.ok(userService.getMyPage(email));
    }

    @PostMapping("/ads/reward")
    public ApiResponse<Void> rewardTicket(
            @RequestHeader("X-Username") String email
    ) {
        userService.rewardTicket(email);
        return ApiResponse.ok();
    }
}
