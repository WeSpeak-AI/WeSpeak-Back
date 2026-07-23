package backend.module.user.controller;

import backend.core.common.response.ApiResponse;
import backend.module.user.dto.TicketConsumeRequest;
import backend.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 서비스 간 내부 호출 전용 엔드포인트. 게이트웨이가 외부로 라우팅하지 않는 경로("/internal/**")이며
 * 도커 네트워크 내부에서 서비스 간 직접 호출로만 사용된다(contracts/internal-apis.md 참고).
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PostMapping("/tickets/consume")
    public ApiResponse<Void> consumeTicket(@Valid @RequestBody TicketConsumeRequest request) {
        userService.consumeTicket(request.email());
        return ApiResponse.ok();
    }
}
