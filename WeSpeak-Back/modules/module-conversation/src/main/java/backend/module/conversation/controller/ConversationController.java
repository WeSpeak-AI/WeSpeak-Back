package backend.module.conversation.controller;

import backend.core.common.response.ApiResponse;
import backend.module.conversation.dto.ConversationRequest;
import backend.module.conversation.dto.TopicResponse;
import backend.module.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/topics")
    public ApiResponse<List<TopicResponse>> getTopics() {
        return ApiResponse.ok(conversationService.getTopics());
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> startConversation(
            @RequestHeader("X-Username") String email,
            @Valid @RequestBody ConversationRequest request
    ) {
        return ApiResponse.ok(conversationService.startConversation(email, request));
    }

    @PatchMapping("/sessions/{conversationId}/close")
    public ApiResponse<Void> closeSession(@PathVariable("conversationId") Long conversationId) {
        conversationService.closeSession(conversationId);
        return ApiResponse.ok();
    }
}
