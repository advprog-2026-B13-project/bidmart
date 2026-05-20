package id.ac.ui.cs.advprog.bidmartcore.payment.service;

import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    TopUpResponse createTopUpTransaction(UUID userId, BigDecimal amount, String paymentType, String bank);
    PaymentNotificationResponse handleNotification(PaymentNotificationRequest payload);
}