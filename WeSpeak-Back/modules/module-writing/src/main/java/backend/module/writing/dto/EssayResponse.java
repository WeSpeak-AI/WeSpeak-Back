package backend.module.writing.dto;

import backend.core.domain.writing.Essay;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EssayResponse {

    private Long essayId;
    private String topic;
    private String content;
    private String type;
    private boolean hasCorrected;
    private LocalDateTime createdAt;

    public static EssayResponse from(Essay essay) {
        return EssayResponse.builder()
                .essayId(essay.getEssayId())
                .topic(essay.getTopic())
                .content(essay.getContent())
                .type(essay.getType().name())
                .hasCorrected(essay.isHasCorrected())
                .createdAt(essay.getCreatedAt())
                .build();
    }
}
