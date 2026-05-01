package backend.module.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadingAiResponse(
        @JsonProperty("user_text") String userText,
        @JsonProperty("feedback_text") String feedbackText,
        @JsonProperty("audio_data") String audioData
) {}
