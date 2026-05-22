package id.ac.ui.cs.advprog.bidmartcore.payment.service;

import com.midtrans.service.MidtransCoreApi;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentAction;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentStatus;
import id.ac.ui.cs.advprog.bidmartcore.payment.repository.PaymentSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentSpringRepository paymentRepository;
    private final WalletService walletService;
    private final MidtransCoreApi midtransCoreApi;

    private static final String BANK_TRANSFER = "bank_transfer";
    private static final String GOPAY = "gopay";

    @Value("${midtrans.server-key}")
    private String serverKey;

    private static final Logger AUDIT = LoggerFactory.getLogger("id.ac.ui.cs.advprog.bidmartcore.AUDIT");

    @Override
    public TopUpResponse createTopUpTransaction(UUID userId, BigDecimal amount, String paymentType, String bank) {
        log.info("Top-up requested: userId={} amount={} paymentType={} bank={}", userId, amount, paymentType, bank);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be greater than zero");
        }

        int grossAmount;
        try {
            grossAmount = amount.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Top up amount must be a whole number in IDR");
        }

        String orderId = "TOPUP-" + UUID.randomUUID();
        PaymentModel payment = PaymentModel.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        String resolvedPaymentType = (paymentType == null || paymentType.isBlank())
                ? BANK_TRANSFER
                : paymentType.toLowerCase(Locale.ROOT);
        String bankCode = (bank == null || bank.isBlank()) ? "bca" : bank.toLowerCase(Locale.ROOT);

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", orderId);
        transactionDetails.put("gross_amount", grossAmount);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("transaction_details", transactionDetails);
        requestBody.put("payment_type", resolvedPaymentType);

        String responseBank = resolvedPaymentType;
        switch (resolvedPaymentType) {
            case "bank_transfer" -> {
                Map<String, Object> bankTransfer = new HashMap<>();
                bankTransfer.put("bank", bankCode);
                requestBody.put(BANK_TRANSFER, bankTransfer);
                responseBank = bankCode;
            }
            case "qris" -> {
                Map<String, Object> qris = new HashMap<>();
                String acquirer = (bank == null || bank.isBlank()) ? GOPAY : bankCode;
                qris.put("acquirer", acquirer);
                requestBody.put("qris", qris);
                responseBank = acquirer;
            }
            case "gopay" -> requestBody.put(GOPAY, new HashMap<>());
            case "shopeepay" -> requestBody.put("shopeepay", new HashMap<>());
            default -> throw new IllegalArgumentException("Unsupported payment type: " + paymentType);
        }

        log.info("Charging Midtrans transaction: orderId={} grossAmount={}", orderId, grossAmount);
        try {
            JSONObject response = midtransCoreApi.chargeTransaction(requestBody);
            String responsePaymentType = response.optString("payment_type", resolvedPaymentType);
            String transactionStatus = response.optString("transaction_status", "pending");

            String vaNumber = null;
            if (BANK_TRANSFER.equals(resolvedPaymentType)) {
                if (response.has("va_numbers")) {
                    JSONArray vaNumbers = response.getJSONArray("va_numbers");
                    if (!vaNumbers.isEmpty()) {
                        JSONObject first = vaNumbers.getJSONObject(0);
                        vaNumber = first.optString("va_number", null);
                        responseBank = first.optString("bank", bankCode);
                    }
                } else if (response.has("permata_va_number")) {
                    vaNumber = response.optString("permata_va_number", null);
                    responseBank = "permata";
                }
            } else if ("qris".equals(resolvedPaymentType)) {
                responseBank = response.optString("acquirer", responseBank);
            }

            List<PaymentAction> actions = parseActions(response);

            log.info("Midtrans charge success: orderId={} status={} paymentType={}", orderId, transactionStatus, responsePaymentType);
            return new TopUpResponse(orderId, responsePaymentType, responseBank, vaNumber, transactionStatus, actions);
        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.error("Midtrans charge failed: orderId={} userId={} amount={}", orderId, userId, amount, ex);
            throw new IllegalStateException("Failed to create Midtrans transaction", ex);
        }
    }

    @Override
    public PaymentNotificationResponse handleNotification(PaymentNotificationRequest payload) {
        if (payload == null || payload.getOrderId() == null) {
            log.warn("Payment notification rejected: missing order_id");
            throw new IllegalArgumentException("order_id is required");
        }

        log.info("Payment notification received: orderId={} status={} grossAmount={}",
                payload.getOrderId(), payload.getTransactionStatus(), payload.getGrossAmount());

        if (!isValidSignature(payload)) {
            log.error("Payment notification INVALID SIGNATURE: orderId={}", payload.getOrderId());
            AUDIT.error("PAYMENT_SIGNATURE_INVALID orderId={}", payload.getOrderId());
            return new PaymentNotificationResponse(payload.getOrderId(), PaymentStatus.FAILED.name(), "invalid signature");
        }

        PaymentModel payment = paymentRepository.findByOrderId(payload.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        PaymentStatus nextStatus = mapStatus(payload.getTransactionStatus());

        PaymentStatus previousStatus = payment.getStatus();

        payment.setStatus(nextStatus);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (nextStatus == PaymentStatus.SUCCESS && previousStatus != PaymentStatus.SUCCESS) {
            log.info("Payment SUCCESS - depositing to wallet: orderId={} userId={} amount={}",
                    payment.getOrderId(), payment.getUserId(), payment.getAmount());
            AUDIT.info("PAYMENT_SUCCESS orderId={} userId={} amount={}", payment.getOrderId(), payment.getUserId(), payment.getAmount());
            walletService.deposit(payment.getUserId(), payment.getAmount());
            return new PaymentNotificationResponse(payment.getOrderId(), nextStatus.name(), "wallet updated");
        }

        if (nextStatus == PaymentStatus.FAILED && previousStatus == PaymentStatus.SUCCESS) {
            log.warn("Payment FAILED after SUCCESS - attempting reversal: orderId={} userId={} amount={}",
                    payment.getOrderId(), payment.getUserId(), payment.getAmount());
            AUDIT.warn("PAYMENT_REVERSAL orderId={} userId={} amount={}", payment.getOrderId(), payment.getUserId(), payment.getAmount());
            try {
                walletService.withdraw(payment.getUserId(), payment.getAmount());
                return new PaymentNotificationResponse(payment.getOrderId(), nextStatus.name(), "wallet reversed");
            } catch (IllegalArgumentException ex) {
                log.error("Payment reversal FAILED - insufficient balance: orderId={} userId={} amount={}",
                        payment.getOrderId(), payment.getUserId(), payment.getAmount(), ex);
                AUDIT.error("PAYMENT_REVERSAL_FAILED orderId={} userId={} amount={}", payment.getOrderId(), payment.getUserId(), payment.getAmount());
                return new PaymentNotificationResponse(payment.getOrderId(), nextStatus.name(), "wallet reversal pending");
            }
        }

        log.info("Payment status updated: orderId={} {} -> {}", payment.getOrderId(), previousStatus, nextStatus);
        return new PaymentNotificationResponse(payment.getOrderId(), nextStatus.name(), "payment updated");
    }

    private PaymentStatus mapStatus(String transactionStatus) {
        if (transactionStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (transactionStatus) {
            case "settlement", "capture", "success" -> PaymentStatus.SUCCESS;
            case "cancel", "deny", "expire", "refund", "partial_refund", "chargeback" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private List<PaymentAction> parseActions(JSONObject response) {
        JSONArray actions = response.optJSONArray("actions");
        if (actions == null || actions.length() == 0) {
            return List.of();
        }

        List<PaymentAction> result = new ArrayList<>();
        for (int i = 0; i < actions.length(); i++) {
            JSONObject action = actions.optJSONObject(i);
            if (action == null) {
                continue;
            }
            result.add(new PaymentAction(
                    action.optString("name", ""),
                    action.optString("method", "GET"),
                    action.optString("url", "")
            ));
        }
        return result;
    }

    private boolean isValidSignature(PaymentNotificationRequest payload) {
        if (payload.getSignatureKey() == null) {
            return false;
        }
        String raw = payload.getOrderId() + payload.getStatusCode() + payload.getGrossAmount() + serverKey;
        String digest = sha512(raw);
        return payload.getSignatureKey().equalsIgnoreCase(digest);
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hashed = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute signature", ex);
        }
    }
}