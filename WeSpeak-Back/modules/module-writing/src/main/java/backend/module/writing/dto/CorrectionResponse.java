package backend.module.writing.dto;

import backend.core.domain.writing.Essay;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CorrectionResponse {

    private Long essayId;
    private String topic;
    private String originalContent;
    private String correctionResult;   // Claude가 반환한 교정 피드백

    public static CorrectionResponse from(Essay essay) {
        return CorrectionResponse.builder()
                .essayId(essay.getEssayId())
                .topic(essay.getTopic())
                .originalContent(essay.getContent())
                .correctionResult(essay.getCorrectionResult())
                .build();
    }
}
