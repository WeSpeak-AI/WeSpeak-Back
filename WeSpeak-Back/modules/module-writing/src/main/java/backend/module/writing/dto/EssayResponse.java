package backend.module.writing.dto;

import backend.module.writing.domain.Essay;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EssayResponse {

    private String essayId;
    private String topic;
    private String content;
    private String type;
    private boolean hasCorrected;
    private LocalDateTime createdAt;

    public static EssayResponse from(Essay essay) {
        return EssayResponse.builder()
                .essayId(String.valueOf(essay.getEssayId()))
                .topic(essay.getTopic())
                .content(essay.getContent())
                .type(essay.getType().name())
                .hasCorrected(essay.isHasCorrected())
                .createdAt(essay.getCreatedAt())
                .build();
    }
}
