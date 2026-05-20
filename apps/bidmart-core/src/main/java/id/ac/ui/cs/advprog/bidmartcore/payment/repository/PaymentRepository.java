package id.ac.ui.cs.advprog.bidmartcore.payment.repository;

import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;

import java.util.Optional;

public interface PaymentRepository {
    PaymentModel save(PaymentModel payment);
    Optional<PaymentModel> findByOrderId(String orderId);
}