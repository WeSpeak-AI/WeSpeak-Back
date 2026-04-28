package backend.core.common.event.payload;

import backend.core.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCorrectionEventPayload implements EventPayload {
    private Long essayId;
    private String content;
}
