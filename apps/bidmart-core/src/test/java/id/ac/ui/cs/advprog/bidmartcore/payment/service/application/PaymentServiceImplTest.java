package id.ac.ui.cs.advprog.bidmartcore.payment.application.service;

import com.midtrans.service.MidtransCoreApi;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentModel;
import id.ac.ui.cs.advprog.bidmartcore.payment.model.PaymentStatus;
import id.ac.ui.cs.advprog.bidmartcore.payment.repository.PaymentSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentServiceImpl;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentSpringRepository paymentRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private MidtransCoreApi midtransCoreApi;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static final String SERVER_KEY = "dummy-server-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "serverKey", SERVER_KEY);

        lenient().when(paymentRepository.save(any(PaymentModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTopUpTransactionShouldCreateBankTransferTransaction() throws Exception {
        UUID userId = UUID.randomUUID();

        JSONObject response = new JSONObject();
        response.put("payment_type", "bank_transfer");
        response.put("transaction_status", "pending");

        JSONArray vaNumbers = new JSONArray();
        vaNumbers.put(new JSONObject()
                .put("bank", "bca")
                .put("va_number", "1234567890"));

        response.put("va_numbers", vaNumbers);

        when(midtransCoreApi.chargeTransaction(anyMap()))
                .thenReturn(response);

        TopUpResponse result = paymentService.createTopUpTransaction(
                userId,
                BigDecimal.valueOf(10000),
                "bank_transfer",
                "bca"
        );

        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("TOPUP-"));
        assertEquals("bank_transfer", result.getPaymentType());
        assertEquals("bca", result.getBank());
        assertEquals("1234567890", result.getVaNumber());
        assertEquals("pending", result.getTransactionStatus());

        ArgumentCaptor<PaymentModel> captor =
                ArgumentCaptor.forClass(PaymentModel.class);

        verify(paymentRepository).save(captor.capture());

        PaymentModel savedPayment = captor.getValue();
        assertEquals(userId, savedPayment.getUserId());
        assertEquals(BigDecimal.valueOf(10000), savedPayment.getAmount());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());

        verify(midtransCoreApi).chargeTransaction(anyMap());
    }

    @Test
    void createTopUpTransactionShouldRejectZeroAmount() {
        UUID userId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createTopUpTransaction(
                        userId,
                        BigDecimal.ZERO,
                        "bank_transfer",
                        "bca"
                )
        );

        assertEquals("Top up amount must be greater than zero", exception.getMessage());
        verifyNoInteractions(midtransCoreApi);
    }

    @Test
    void createTopUpTransactionShouldRejectDecimalAmount() {
        UUID userId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createTopUpTransaction(
                        userId,
                        BigDecimal.valueOf(10000.50),
                        "bank_transfer",
                        "bca"
                )
        );

        assertEquals("Top up amount must be a whole number in IDR", exception.getMessage());
        verifyNoInteractions(midtransCoreApi);
    }

    @Test
    void createTopUpTransactionShouldRejectUnsupportedPaymentType() {
        UUID userId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createTopUpTransaction(
                        userId,
                        BigDecimal.valueOf(10000),
                        "unknown_payment",
                        "bca"
                )
        );

        assertEquals("Unsupported payment type: unknown_payment", exception.getMessage());
        verifyNoInteractions(midtransCoreApi);
    }

    @Test
    void createTopUpTransactionShouldMarkPaymentAsFailedWhenMidtransThrowsError() throws Exception {
        UUID userId = UUID.randomUUID();

        when(midtransCoreApi.chargeTransaction(anyMap()))
                .thenThrow(new RuntimeException("Midtrans error"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> paymentService.createTopUpTransaction(
                        userId,
                        BigDecimal.valueOf(10000),
                        "bank_transfer",
                        "bca"
                )
        );

        assertEquals("Failed to create Midtrans transaction", exception.getMessage());

        ArgumentCaptor<PaymentModel> captor =
                ArgumentCaptor.forClass(PaymentModel.class);

        verify(paymentRepository, times(2)).save(captor.capture());

        PaymentModel failedPayment = captor.getAllValues().get(1);
        assertEquals(PaymentStatus.FAILED, failedPayment.getStatus());
        assertNotNull(failedPayment.getUpdatedAt());
    }

    @Test
    void handleNotificationShouldUpdateWalletWhenSettlementIsValid() {
        UUID userId = UUID.randomUUID();
        String orderId = "TOPUP-123";
        BigDecimal amount = BigDecimal.valueOf(10000);

        PaymentModel payment = PaymentModel.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        PaymentNotificationRequest request = createNotification(
                orderId,
                "settlement",
                "200",
                "10000.00"
        );

        PaymentNotificationResponse response =
                paymentService.handleNotification(request);

        assertEquals(orderId, response.getOrderId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("wallet updated", response.getMessage());

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(walletService).deposit(userId, amount);
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleNotificationShouldNotDepositAgainWhenPaymentAlreadySuccess() {
        UUID userId = UUID.randomUUID();
        String orderId = "TOPUP-123";

        PaymentModel payment = PaymentModel.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        PaymentNotificationRequest request = createNotification(
                orderId,
                "settlement",
                "200",
                "10000.00"
        );

        PaymentNotificationResponse response =
                paymentService.handleNotification(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("payment updated", response.getMessage());

        verify(walletService, never()).deposit(any(), any());
    }

    @Test
    void handleNotificationShouldReturnInvalidSignatureWhenSignatureIsWrong() {
        PaymentNotificationRequest request = new PaymentNotificationRequest();
        request.setOrderId("TOPUP-123");
        request.setTransactionStatus("settlement");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setSignatureKey("wrong-signature");

        PaymentNotificationResponse response =
                paymentService.handleNotification(request);

        assertEquals("TOPUP-123", response.getOrderId());
        assertEquals("FAILED", response.getStatus());
        assertEquals("invalid signature", response.getMessage());

        verify(paymentRepository, never()).findByOrderId(anyString());
        verify(walletService, never()).deposit(any(), any());
    }

    private PaymentNotificationRequest createNotification(
            String orderId,
            String transactionStatus,
            String statusCode,
            String grossAmount
    ) {
        PaymentNotificationRequest request = new PaymentNotificationRequest();
        request.setOrderId(orderId);
        request.setTransactionStatus(transactionStatus);
        request.setStatusCode(statusCode);
        request.setGrossAmount(grossAmount);
        request.setSignatureKey(generateSignature(orderId, statusCode, grossAmount));
        return request;
    }

    private String generateSignature(String orderId, String statusCode, String grossAmount) {
        try {
            String raw = orderId + statusCode + grossAmount + SERVER_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}