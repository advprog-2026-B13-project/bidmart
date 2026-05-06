package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registration result payload")
public class RegisterResponse {

    @Schema(description = "Unique ID of newly registered user", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Registered email address", example = "user@example.com")
    private String email;

    @Schema(description = "Display name associated to the account", example = "John Doe")
    private String displayName;

    @Schema(description = "Whether email verification is required before login", example = "true")
    private Boolean requiresEmailVerification;

    @Schema(
            description = "Short-lived token to authorize resend verification OTP requests",
            example = "eyJhbGciOiJIUzI1NiJ9..."
    )
    private String verificationToken;

    public static RegisterResponse fromMap(Map<String, Object> data) {
        Object rawUserId = data.get("userId");
        UUID parsedUserId = rawUserId instanceof UUID
                ? (UUID) rawUserId
                : UUID.fromString(String.valueOf(rawUserId));

        return new RegisterResponse(
                parsedUserId,
                (String) data.get("email"),
                (String) data.get("displayName"),
                (Boolean) data.getOrDefault("requiresEmailVerification", Boolean.TRUE),
                (String) data.get("verificationToken")
        );
    }
}
