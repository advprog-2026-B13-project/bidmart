package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for updating user profile")
public class ProfileUpdateRequest {

    @Schema(description = "New display name (null to keep current)", example = "Jane Doe")
    private String displayName;

    @Schema(description = "New profile photo URL (null to keep current)", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "New shipping address (null to keep current)", example = "Jl. Margonda Raya No. 100, Depok")
    private String shippingAddress;
}
