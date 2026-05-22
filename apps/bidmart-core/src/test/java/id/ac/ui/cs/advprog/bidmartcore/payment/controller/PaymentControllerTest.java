package id.ac.ui.cs.advprog.bidmartcore.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.PaymentAction;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmartcore.payment.controller.dto.TopUpResponse;
import id.ac.ui.cs.advprog.bidmartcore.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService))
                .build();
    }

    @Test
    void topUpShouldReturnTopUpResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(10000));
        request.setPaymentType("bank_transfer");
        request.setBank("bca");

        TopUpResponse response = new TopUpResponse(
                "TOPUP-123",
                "bank_transfer",
                "bca",
                "1234567890",
                "pending",
                List.of()
        );

        when(paymentService.createTopUpTransaction(
                eq(userId),
                eq(BigDecimal.valueOf(10000)),
                eq("bank_transfer"),
                eq("bca")
        )).thenReturn(response);

        mockMvc.perform(post("/api/payment/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOPUP-123")))
                .andExpect(content().string(containsString("bank_transfer")))
                .andExpect(content().string(containsString("bca")))
                .andExpect(content().string(containsString("1234567890")));

        verify(paymentService).createTopUpTransaction(
                eq(userId),
                eq(BigDecimal.valueOf(10000)),
                eq("bank_transfer"),
                eq("bca")
        );
    }

    @Test
    void topUpShouldReturnQrisActionResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(BigDecimal.valueOf(15000));
        request.setPaymentType("qris");
        request.setBank("gopay");

        TopUpResponse response = new TopUpResponse(
                "TOPUP-QRIS-123",
                "qris",
                "gopay",
                null,
                "pending",
                List.of(new PaymentAction(
                        "generate-qr-code",
                        "GET",
                        "https://example.com/qr"
                ))
        );

        when(paymentService.createTopUpTransaction(
                eq(userId),
                eq(BigDecimal.valueOf(15000)),
                eq("qris"),
                eq("gopay")
        )).thenReturn(response);

        mockMvc.perform(post("/api/payment/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TOPUP-QRIS-123")))
                .andExpect(content().string(containsString("qris")))
                .andExpect(content().string(containsString("generate-qr-code")))
                .andExpect(content().string(containsString("https://example.com/qr")));

        verify(paymentService).createTopUpTransaction(
                eq(userId),
                eq(BigDecimal.valueOf(15000)),
                eq("qris"),
                eq("gopay")
        );
    }
}