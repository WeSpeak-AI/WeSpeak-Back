package backend.module.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketConsumeRequest(
        @NotBlank String email
) {
}
