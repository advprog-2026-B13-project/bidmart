package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentNotificationResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentCallbackController(paymentService))
                .build();
    }

    @Test
    void handleCallbackShouldProcessNotificationFromCallbackPath() throws Exception {
        PaymentNotificationRequest request = createRequest();

        PaymentNotificationResponse response = new PaymentNotificationResponse(
                "TOPUP-123",
                "SUCCESS",
                "wallet updated"
        );

        when(paymentService.handleNotification(any(PaymentNotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/payment/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOPUP-123")))
                .andExpect(content().string(containsString("SUCCESS")))
                .andExpect(content().string(containsString("wallet updated")));

        verify(paymentService).handleNotification(any(PaymentNotificationRequest.class));
    }

    @Test
    void handleCallbackShouldProcessNotificationFromNotificationPath() throws Exception {
        PaymentNotificationRequest request = createRequest();

        PaymentNotificationResponse response = new PaymentNotificationResponse(
                "TOPUP-123",
                "PENDING",
                "payment updated"
        );

        when(paymentService.handleNotification(any(PaymentNotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/payment/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOPUP-123")))
                .andExpect(content().string(containsString("PENDING")))
                .andExpect(content().string(containsString("payment updated")));

        verify(paymentService).handleNotification(any(PaymentNotificationRequest.class));
    }

    @Test
    void handleCallbackShouldProcessNotificationFromMidtransCallbackPath() throws Exception {
        PaymentNotificationRequest request = createRequest();

        PaymentNotificationResponse response = new PaymentNotificationResponse(
                "TOPUP-123",
                "FAILED",
                "wallet reversed"
        );

        when(paymentService.handleNotification(any(PaymentNotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/payment/midtrans/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOPUP-123")))
                .andExpect(content().string(containsString("FAILED")))
                .andExpect(content().string(containsString("wallet reversed")));

        verify(paymentService).handleNotification(any(PaymentNotificationRequest.class));
    }

    private PaymentNotificationRequest createRequest() {
        PaymentNotificationRequest request = new PaymentNotificationRequest();
        request.setOrderId("TOPUP-123");
        request.setTransactionStatus("settlement");
        request.setStatusCode("200");
        request.setGrossAmount("10000.00");
        request.setSignatureKey("signature");
        return request;
    }
}