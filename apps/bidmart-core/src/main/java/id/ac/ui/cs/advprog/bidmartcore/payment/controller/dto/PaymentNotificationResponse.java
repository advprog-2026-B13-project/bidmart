package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Payment notification processing result")
public class PaymentNotificationResponse {

    @Schema(description = "Midtrans order ID")
    private String orderId;

    @Schema(description = "Payment status", example = "SUCCESS")
    private String status;

    @Schema(description = "Result message", example = "wallet updated")
    private String message;
}

