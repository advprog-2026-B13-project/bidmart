package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest.ApiResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Top up and payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/topup")
    @Operation(summary = "Create a top up transaction")
    public ResponseEntity<ApiResponse<TopUpResponse>> topUp(@Valid @RequestBody TopUpRequest request) {
        TopUpResponse response = paymentService.createTopUpTransaction(
                request.getUserId(),
                request.getAmount(),
            request.getPaymentType(),
                request.getBank()
        );
        return ResponseEntity.ok(ApiResponse.success("Top up created", response));
    }
}