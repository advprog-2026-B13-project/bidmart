package id.ac.ui.cs.advprog.bidmartcore.payment.repository;

import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSpringRepository extends JpaRepository<PaymentModel, UUID> {
    Optional<PaymentModel> findByOrderId(String orderId);
}