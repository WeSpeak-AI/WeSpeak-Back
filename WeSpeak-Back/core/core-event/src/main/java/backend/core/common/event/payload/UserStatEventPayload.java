package backend.core.common.event.payload;

import backend.core.common.event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatEventPayload implements EventPayload {
    private String email;
    private LocalDateTime recordedAt;
}
