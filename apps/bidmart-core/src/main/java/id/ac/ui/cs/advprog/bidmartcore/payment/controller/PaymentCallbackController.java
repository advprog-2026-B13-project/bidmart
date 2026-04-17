package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import id.ac.ui.cs.advprog.bidmartcore.payment.repository.PaymentSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payment")
public class PaymentCallbackController {

    @Autowired
    private PaymentSpringRepository paymentRepository;

    @Autowired
    private WalletService walletService;

    public PaymentCallbackController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/callback")
    public void handleCallback(@RequestBody Map<String, Object> payload) {

        String orderId = (String) payload.get("order_id");
        String status = (String) payload.get("transaction_status");

        if (!"settlement".equals(status)) return;

        PaymentModel payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow();

        payment.setStatus("SUCCESS");
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        walletService.deposit(
                payment.getUserId(),
                payment.getAmount()
        );
    }
}