package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body to confirm replacing an old active session when session limit is reached")
public class SessionReplacementConfirmationRequest {

    @Schema(description = "Session replacement token returned from login response", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionReplacementToken;

    @Schema(description = "Whether client confirms replacing the oldest active session", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean replaceOldestSession;
}

