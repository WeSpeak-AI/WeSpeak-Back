package backend.module.writing.controller;

import backend.core.common.response.ApiResponse;
import backend.module.writing.service.AdminWritingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 교정 실패(FAILED) 에세이 재처리용 운영자 API. ADMIN 권한 검사는 게이트웨이(JwtGlobalFilter)가 수행한다.
 */
@RestController
@RequestMapping("/api/admin/essays")
@RequiredArgsConstructor
public class AdminWritingController {

    private final AdminWritingService adminWritingService;

    // 에러: W001(에세이 없음), W004(FAILED 상태가 아님)
    @PostMapping("/{essayId}/correction/retry")
    public ApiResponse<Void> retryCorrection(@PathVariable("essayId") Long essayId) {
        adminWritingService.retryCorrection(essayId);
        return ApiResponse.ok();
    }

    // 응답: 이번 호출에서 재요청한 건수(최대 100). 0이 될 때까지 반복 호출한다.
    @PostMapping("/correction/retry-failed")
    public ApiResponse<Integer> retryFailedCorrections() {
        return ApiResponse.ok(adminWritingService.retryFailedCorrections());
    }
}
