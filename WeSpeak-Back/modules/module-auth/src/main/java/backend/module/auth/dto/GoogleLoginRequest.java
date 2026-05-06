package backend.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GoogleLoginRequest {

    @NotBlank
    private String idToken;
}
