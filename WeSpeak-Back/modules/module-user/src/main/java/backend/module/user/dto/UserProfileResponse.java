package backend.module.user.dto;

import backend.core.domain.user.User;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserProfileResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String email;
    private String nickname;
    private int xp;
    private int streak;
    private int mediaTicket;
    private LocalDate lastStudiedAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .xp(user.getXp())
                .streak(user.getStreak())
                .mediaTicket(user.getMediaTicket())
                .lastStudiedAt(user.getLastStudiedAt())
                .build();
    }
}
