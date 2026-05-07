package backend.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EditProfileRequest {

    @NotBlank
    private String nickname;
}
