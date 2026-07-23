package backend.module.writing.dto;

import backend.module.writing.domain.Essay;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CorrectionResponse {

    private String essayId;
    private String topic;
    private String originalContent;
    private String correctionResult;   // Claude가 반환한 교정 피드백

    public static CorrectionResponse from(Essay essay) {
        return CorrectionResponse.builder()
                .essayId(String.valueOf(essay.getEssayId()))
                .topic(essay.getTopic())
                .originalContent(essay.getContent())
                .correctionResult(essay.getCorrectionResult())
                .build();
    }
}
