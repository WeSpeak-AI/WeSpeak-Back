package backend.core.common.event.payload;

import backend.core.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyCompletedEventPayload implements EventPayload {
    private String email;
    private LocalDate studiedAt;
}
