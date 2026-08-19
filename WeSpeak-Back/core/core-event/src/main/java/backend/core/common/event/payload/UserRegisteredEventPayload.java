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
public class UserRegisteredEventPayload implements EventPayload {
    private Long userId;
    private String email;
    private String nickname;
    private LocalDateTime registeredAt;
}
