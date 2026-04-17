package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/topup")
    public Map<String, String> topUp(@RequestParam UUID userId,
                                     @RequestParam double amount) {

        String url = paymentService.createTopUpTransaction(userId, amount);

        return Map.of("payment_url", url);
    }
}