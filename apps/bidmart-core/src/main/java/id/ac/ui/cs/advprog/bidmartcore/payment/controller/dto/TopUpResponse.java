package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Top up transaction response")
public class TopUpResponse {

    @Schema(description = "Midtrans order ID")
    private String orderId;

    @Schema(description = "Payment type", example = "bank_transfer")
    private String paymentType;

    @Schema(description = "Bank code", example = "bca")
    private String bank;

    @Schema(description = "Virtual account number")
    private String vaNumber;

    @Schema(description = "Transaction status", example = "pending")
    private String transactionStatus;

    @Schema(description = "Midtrans actions for QR/e-wallet flows")
    private List<PaymentAction> actions;
}

