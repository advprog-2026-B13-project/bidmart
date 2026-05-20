package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Midtrans action payload")
public class PaymentAction {

    @Schema(description = "Action name", example = "generate-qr-code")
    private String name;

    @Schema(description = "HTTP method", example = "GET")
    private String method;

    @Schema(description = "Action URL")
    private String url;
}
