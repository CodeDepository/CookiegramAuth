package org.example.cookiegram.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.example.cookiegram.auth.security.AuthenticatedUser;
import org.example.cookiegram.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PaymentService paymentService;
    // AuthFilter is included in @WebMvcTest - mock its AuthService dependency
    @MockBean AuthService authService;

    private static final String TOKEN    = "test-session-token";
    private static final AuthenticatedUser CUSTOMER =
            new AuthenticatedUser(1L, "alice", "alice@test.com", "CUSTOMER");

    @BeforeEach
    void auth() {
        when(authService.requireUserByToken(TOKEN)).thenReturn(CUSTOMER);
    }

    // GET /api/payment/config

    @Test
    @DisplayName("GET /config returns clientId and configured=true")
    void config_returns_client_id() throws Exception {
        when(paymentService.getClientId()).thenReturn("AaBbCcDdSandboxClientId");
        when(paymentService.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/payment/config")
                        .cookie(new Cookie("CG_SESSION", TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("AaBbCcDdSandboxClientId"))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    @DisplayName("GET /config returns configured=false when PayPal keys not set")
    void config_not_configured() throws Exception {
        when(paymentService.getClientId()).thenReturn("");
        when(paymentService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/payment/config")
                        .cookie(new Cookie("CG_SESSION", TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    @DisplayName("GET /config without session cookie returns 401")
    void config_requires_auth() throws Exception {
        mockMvc.perform(get("/api/payment/config"))
                .andExpect(status().isUnauthorized());
    }

    // POST /api/payment/create-order

    @Test
    @DisplayName("POST /create-order returns PayPal orderID for valid amount")
    void create_order_returns_order_id() throws Exception {
        when(paymentService.createOrder(any(BigDecimal.class)))
                .thenReturn("PAYPAL_ORDER_ABC123");

        mockMvc.perform(post("/api/payment/create-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 11.98))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderID").value("PAYPAL_ORDER_ABC123"));
    }

    @Test
    @DisplayName("POST /create-order returns 400 when amount is missing")
    void create_order_requires_amount() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("amount is required"));
    }

    @Test
    @DisplayName("POST /create-order returns 400 when amount is zero")
    void create_order_rejects_zero_amount() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount must be positive"));
    }

    @Test
    @DisplayName("POST /create-order returns 400 when amount is negative")
    void create_order_rejects_negative_amount() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", -5.99))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Amount must be positive"));
    }

    @Test
    @DisplayName("POST /create-order returns 400 when PayPal API throws")
    void create_order_handles_paypal_error() throws Exception {
        when(paymentService.createOrder(any(BigDecimal.class)))
                .thenThrow(new IOException("PayPal connection refused"));

        mockMvc.perform(post("/api/payment/create-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 5.99))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("PayPal connection refused")));
    }

    @Test
    @DisplayName("POST /create-order without session returns 401")
    void create_order_requires_auth() throws Exception {
        mockMvc.perform(post("/api/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 5.99))))
                .andExpect(status().isUnauthorized());
    }

    // POST /api/payment/capture-order

    @Test
    @DisplayName("POST /capture-order returns orderID and COMPLETED status")
    void capture_order_succeeds() throws Exception {
        when(paymentService.captureOrder("PAYPAL_ORDER_ABC123"))
                .thenReturn("PAYPAL_ORDER_ABC123");

        mockMvc.perform(post("/api/payment/capture-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderID", "PAYPAL_ORDER_ABC123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderID").value("PAYPAL_ORDER_ABC123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /capture-order returns 400 when orderID is missing")
    void capture_order_requires_order_id() throws Exception {
        mockMvc.perform(post("/api/payment/capture-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("orderID is required"));
    }

    @Test
    @DisplayName("POST /capture-order returns 400 when PayPal capture fails")
    void capture_order_handles_failure() throws Exception {
        when(paymentService.captureOrder(any()))
                .thenThrow(new IOException("capture failed - status: DECLINED"));

        mockMvc.perform(post("/api/payment/capture-order")
                        .cookie(new Cookie("CG_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderID", "BAD_ORDER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("capture failed")));
    }

    @Test
    @DisplayName("POST /capture-order without session returns 401")
    void capture_order_requires_auth() throws Exception {
        mockMvc.perform(post("/api/payment/capture-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderID", "X"))))
                .andExpect(status().isUnauthorized());
    }
}
