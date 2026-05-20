package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest.ApiResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Top up and payment processing")
public class PaymentCallbackController {

    private final PaymentService paymentService;

        @PostMapping({
            "/callback",
            "/notification",
            "/notifications",
            "/midtrans/callback",
            "/midtrans/notification"
        })
    @Operation(summary = "Handle Midtrans payment notifications")
    public ResponseEntity<ApiResponse<PaymentNotificationResponse>> handleCallback(
            @RequestBody PaymentNotificationRequest payload) {
        PaymentNotificationResponse response = paymentService.handleNotification(payload);
        return ResponseEntity.ok(ApiResponse.success("Notification processed", response));
    }
}