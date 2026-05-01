package backend.module.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiChatResponse(
        @JsonProperty("user_text") String userText,
        @JsonProperty("ai_text") String aiText,
        @JsonProperty("audio_data") String audioData
) {}
