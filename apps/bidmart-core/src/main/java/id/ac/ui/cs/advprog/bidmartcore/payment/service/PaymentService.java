package id.ac.ui.cs.advprog.bidmartcore.payment.service;

import java.util.UUID;

public interface PaymentService {
    String createTopUpTransaction(UUID userId, double amount);
}