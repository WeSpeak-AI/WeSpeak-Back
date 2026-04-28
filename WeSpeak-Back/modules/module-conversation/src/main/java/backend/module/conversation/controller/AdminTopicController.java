package backend.module.conversation.controller;

import backend.core.common.response.ApiResponse;
import backend.module.conversation.dto.TopicRequest;
import backend.module.conversation.service.AdminTopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
public class AdminTopicController {

    private final AdminTopicService adminTopicService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Long> createTopic(@Valid @RequestBody TopicRequest request) {
        return ApiResponse.ok(adminTopicService.createTopic(request));
    }

    @PutMapping("/{topicId}")
    public ApiResponse<Void> updateTopic(@PathVariable("topicId") Long topicId, @Valid @RequestBody TopicRequest request) {
        adminTopicService.updateTopic(topicId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> deleteTopic(@PathVariable("topicId") Long topicId) {
        adminTopicService.deleteTopic(topicId);
        return ApiResponse.ok();
    }
}
