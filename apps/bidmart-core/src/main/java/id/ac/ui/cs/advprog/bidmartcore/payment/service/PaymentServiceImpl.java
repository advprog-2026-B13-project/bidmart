package id.ac.ui.cs.advprog.bidmartcore.payment.service;

import id.ac.ui.cs.advprog.bidmartcore.payment.repository.PaymentSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentSpringRepository paymentRepository;

    @Autowired
    private WalletService walletService;

    @Value("${midtrans.server-key}")
    private String serverKey;

    @Value("${midtrans.is-production}")
    private boolean isProduction;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String createTopUpTransaction(UUID userId, double amount) {

        System.out.println("SERVER KEY = " + serverKey);

        String url = isProduction
                ? "https://api.midtrans.com/v2/charge"
                : "https://api.sandbox.midtrans.com/v2/charge";

        String orderId = UUID.randomUUID().toString();

        PaymentModel payment = PaymentModel.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(amount))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        Map<String, Object> body = new HashMap<>();

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", orderId);
        transactionDetails.put("gross_amount", (int) amount);

        body.put("transaction_details", transactionDetails);
        body.put("payment_type", "bank_transfer");

        Map<String, Object> bankTransfer = new HashMap<>();
        bankTransfer.put("bank", "bca");

        body.put("bank_transfer", bankTransfer);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String encodedKey = Base64.getEncoder()
                .encodeToString((serverKey + ":").getBytes());

        headers.set("Authorization", "Basic " + encodedKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        System.out.println("REQUEST BODY = " + body);
        System.out.println("URL = " + url);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> responseBody = response.getBody();

        System.out.println("RESPONSE = " + responseBody);

        if (responseBody != null) {
            List<Map<String, String>> vaNumbers =
                    (List<Map<String, String>>) responseBody.get("va_numbers");

            if (vaNumbers != null && !vaNumbers.isEmpty()) {
                return vaNumbers.get(0).get("va_number");
            }
        }

        return "NO_VA_NUMBER";
    }
}