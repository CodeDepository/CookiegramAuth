package org.example.cookiegram.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wraps the PayPal Orders v2 API (sandbox mode).
 *
 * Card details are NEVER sent to our server — the PayPal JS SDK renders a
 * hosted popup where the user logs in or enters their card, all on PayPal's
 * own servers.
 *
 * When paypal.client-id / paypal.client-secret are not set (local dev without
 * a PayPal account), the service falls back to a safe demo mode so the rest
 * of the app still works.
 */
@Service
public class PaymentService {

    @Value("${paypal.client-id:}")
    private String clientId;

    @Value("${paypal.client-secret:}")
    private String clientSecret;

    @Value("${paypal.sandbox:true}")
    private boolean sandbox;

    private PayPalHttpClient paypalClient;

    @PostConstruct
    void init() {
        if (isConfigured()) {
            PayPalEnvironment env = sandbox
                    ? new PayPalEnvironment.Sandbox(clientId, clientSecret)
                    : new PayPalEnvironment.Live(clientId, clientSecret);
            paypalClient = new PayPalHttpClient(env);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Creates a PayPal order and returns its ID.
     * The frontend passes this ID to the PayPal JS SDK which opens the payment popup.
     * Falls back to a mock ID in demo mode.
     */
    public String createOrder(BigDecimal amount) throws IOException {
        if (!isConfigured()) {
            return "DEMO_ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }

        String value = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        List<PurchaseUnitRequest> units = new ArrayList<>();
        units.add(new PurchaseUnitRequest()
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode("CAD")
                        .value(value)));

        OrderRequest body = new OrderRequest();
        body.checkoutPaymentIntent("CAPTURE");
        body.purchaseUnits(units);
        body.applicationContext(new ApplicationContext()
                .brandName("Cookiegram")
                .landingPage("NO_PREFERENCE")
                .userAction("PAY_NOW"));

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.prefer("return=representation");
        request.requestBody(body);

        return paypalClient.execute(request).result().id();
    }

    /**
     * Captures an approved PayPal order (called after the user approves in the popup).
     * Returns the order ID on success; throws IOException if the capture fails.
     * Falls back gracefully in demo mode.
     */
    public String captureOrder(String orderID) throws IOException {
        if (!isConfigured()) {
            // Demo: treat any DEMO_ order as instantly captured
            if (orderID != null && orderID.startsWith("DEMO_ORDER_")) {
                return orderID;
            }
            throw new IOException("Demo mode: invalid order ID");
        }

        OrdersCaptureRequest request = new OrdersCaptureRequest(orderID);
        request.requestBody(new OrderActionRequest());

        Order result = paypalClient.execute(request).result();

        if (!"COMPLETED".equalsIgnoreCase(result.status())) {
            throw new IOException("PayPal capture did not complete — status: " + result.status());
        }
        return result.id();
    }

    /**
     * Verifies that a PayPal order has been fully captured (status = COMPLETED).
     * Called by OrderService before persisting the order to the database.
     * In demo mode always returns true.
     */
    public boolean verifySucceeded(String orderID) throws IOException {
        if (!isConfigured()) {
            return true; // demo / dev bypass
        }
        if (orderID == null || orderID.startsWith("DEMO_")) {
            return false; // real keys set but a demo ID was sent — reject
        }

        OrdersGetRequest request = new OrdersGetRequest(orderID);
        Order order = paypalClient.execute(request).result();
        return "COMPLETED".equalsIgnoreCase(order.status());
    }
}
