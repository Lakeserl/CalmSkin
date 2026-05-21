package com.lakeserl.payment_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.payment_service.models.dto.request.PaymentInitiateRequest;
import com.lakeserl.payment_service.models.dto.request.CodConfirmRequest;
import com.lakeserl.payment_service.models.dto.request.RefundInitiateRequest;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.enums.PaymentMethod;
import com.lakeserl.payment_service.models.enums.PaymentStatus;
import com.lakeserl.payment_service.repository.PaymentRepository;
import com.lakeserl.payment_service.repository.RefundRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {

    private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        refundRepository.deleteAll();
    }

    @Test
    void testInitiatePayment_Points_CompletedImmediately() throws Exception {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "POINTS");
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10001")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(0L) // Points standard zero amount
                .method(PaymentMethod.POINTS)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void testInitiatePayment_VNPay_UrlGenerated() throws Exception {
        // Arrange
        PaymentInitiateRequest request = new PaymentInitiateRequest("CS-12345", "VNPAY");
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10002")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/initiate")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testGetPaymentByNumber_Authorized() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10003")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/PAY-10003")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentNumber").value("PAY-10003"));
    }

    @Test
    void testGetPaymentByNumber_Unauthorized_DifferentUser() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10004")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert (Should throw Not Found or Access Denied)
        mockMvc.perform(get("/api/v1/payments/PAY-10004")
                        .header("X-User-Id", "2") // different user
                        .header("X-User-Role", "USER"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPaymentsForUser_Success() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10005")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].paymentNumber").value("PAY-10005"));
    }

    @Test
    void testGetAllPaymentsForAdmin_DeniedForUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllPaymentsForAdmin_AllowedForAdmin() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10006")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/admin")
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentNumber").value("PAY-10006"));
    }

    @Test
    void testConfirmCodPayment_AllowedForAdmin() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10007")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        CodConfirmRequest request = new CodConfirmRequest("CS-12345");

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/cod/confirm")
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void testConfirmCodPayment_DeniedForUser() throws Exception {
        CodConfirmRequest request = new CodConfirmRequest("CS-12345");

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/cod/confirm")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRefundPayment_AuthorizedAdmin() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10008")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.POINTS)
                .status(PaymentStatus.COMPLETED)
                .build();
        paymentRepository.save(payment);

        RefundInitiateRequest request = new RefundInitiateRequest(100000L, "Return package");

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/PAY-10008/refund")
                        .header("X-User-Id", "99")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(100000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void testRefundPayment_DeniedForDifferentUser() throws Exception {
        // Arrange
        Payment payment = Payment.builder()
                .paymentNumber("PAY-10009")
                .orderId(123L)
                .orderNumber("CS-12345")
                .userId(TEST_USER_ID)
                .amount(150000L)
                .method(PaymentMethod.POINTS)
                .status(PaymentStatus.COMPLETED)
                .build();
        paymentRepository.save(payment);

        RefundInitiateRequest request = new RefundInitiateRequest(100000L, "Return package");

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/PAY-10009/refund")
                        .header("X-User-Id", "2") // different user
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testVNPayWebhook_SignatureInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/webhook/vnpay/ipn")
                        .param("vnp_TxnRef", "CS-12345")
                        .param("vnp_SecureHash", "wronghash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    @Test
    void testMomoWebhook_Processed() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"CS-12345\",\"signature\":\"val\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testZaloPayWebhook_Processed() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook/zalopay/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"{\\\"app_id\\\":123}\",\"mac\":\"macval\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.return_code").value(2));
    }
}
