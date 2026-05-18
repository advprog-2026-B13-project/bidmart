package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Midtrans payment notification payload")
public class PaymentNotificationRequest {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("gross_amount")
    private String grossAmount;

    @JsonProperty("signature_key")
    private String signatureKey;
}

