package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Top up request")
public class TopUpRequest {

    @NotNull
    @Schema(description = "User ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID userId;

    @NotNull
    @Positive
    @Schema(description = "Top up amount in IDR (whole number)", example = "10000")
    private BigDecimal amount;

    @Schema(description = "Bank transfer destination", example = "bca")
    private String bank;

    @Schema(description = "Payment type", example = "bank_transfer")
    private String paymentType;
}

