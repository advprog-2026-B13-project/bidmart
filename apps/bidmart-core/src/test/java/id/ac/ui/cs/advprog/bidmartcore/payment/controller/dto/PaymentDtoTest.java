package id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentDtoTest {

    @Test
    void topUpRequestShouldSetAndGetValues() {
        UUID userId = UUID.randomUUID();

        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(10000));
        request.setBank("bca");
        request.setPaymentType("bank_transfer");

        assertEquals(userId, request.getUserId());
        assertEquals(BigDecimal.valueOf(10000), request.getAmount());
        assertEquals("bca", request.getBank());
        assertEquals("bank_transfer", request.getPaymentType());
    }

    @Test
    void topUpResponseShouldStoreValues() {
        PaymentAction action = new PaymentAction(
                "generate-qr-code",
                "GET",
                "https://example.com/qr"
        );

        TopUpResponse response = new TopUpResponse(
                "TOPUP-123",
                "qris",
                "gopay",
                null,
                "pending",
                List.of(action)
        );

        assertEquals("TOPUP-123", response.getOrderId());
        assertEquals("qris", response.getPaymentType());
        assertEquals("gopay", response.getBank());
        assertNull(response.getVaNumber());
        assertEquals("pending", response.getTransactionStatus());
        assertEquals(1, response.getActions().size());
        assertEquals(action, response.getActions().get(0));
    }

    @Test
    void paymentNotificationRequestShouldSetAndGetValues() {
        PaymentNotificationRequest request = new PaymentNotificationRequest();
        request.setOrderId("TOPUP-123");
        request.setTransactionStatus("settlement");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setSignatureKey("signature");

        assertEquals("TOPUP-123", request.getOrderId());
        assertEquals("settlement", request.getTransactionStatus());
        assertEquals("200", request.getStatusCode());
        assertEquals("10000.00", request.getGrossAmount());
        assertEquals("signature", request.getSignatureKey());
    }

    @Test
    void paymentNotificationResponseShouldStoreValues() {
        PaymentNotificationResponse response = new PaymentNotificationResponse(
                "TOPUP-123",
                "SUCCESS",
                "wallet updated"
        );

        assertEquals("TOPUP-123", response.getOrderId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("wallet updated", response.getMessage());
    }

    @Test
    void paymentActionShouldStoreValues() {
        PaymentAction action = new PaymentAction(
                "generate-qr-code",
                "GET",
                "https://example.com/qr"
        );

        assertEquals("generate-qr-code", action.getName());
        assertEquals("GET", action.getMethod());
        assertEquals("https://example.com/qr", action.getUrl());
    }
}