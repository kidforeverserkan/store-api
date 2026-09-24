package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.orders.OrderRepository;
import com.kidforeverserkan.store.users.Role;
import com.kidforeverserkan.store.users.User;
import com.kidforeverserkan.store.users.UserRepository;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the Stripe webhook endpoint over a real HTTP connection to the
 * embedded Tomcat (not MockMvc), so header handling is exactly what Stripe
 * and the Stripe CLI hit in practice: the signature arrives as
 * "Stripe-Signature" and is verified against the test-only webhook secret.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:store_webhook_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class StripeWebhookTests {

    @LocalServerPort
    private int port;

    @Value("${stripe.webhookSecretKey}")
    private String webhookSecret;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private Order order;

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setName("Webhook Customer");
        user.setEmail("webhook-" + UUID.randomUUID() + "@example.com");
        user.setPassword("not-used");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        order = new Order();
        order.setCustomer(user);
        order.setStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("10.00"));
        order = orderRepository.save(order);
    }

    private String event(String type, Long orderId) {
        return """
                {"id":"evt_test","object":"event","api_version":"%s","type":"%s",
                 "data":{"object":{"id":"pi_test","object":"payment_intent",
                 "metadata":{"order_id":"%d"}}}}
                """.formatted(Stripe.API_VERSION, type, orderId);
    }

    private String sign(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        var signature = Webhook.Util.computeHmacSha256(webhookSecret, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + signature;
    }

    private HttpResponse<String> post(String payload, String signatureHeader) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/checkout/webhook"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (signatureHeader != null) {
            builder.header("Stripe-Signature", signatureHeader);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private PaymentStatus currentStatus() {
        return orderRepository.findById(order.getId()).orElseThrow().getStatus();
    }

    @Test
    void paymentSucceeded_withValidSignature_marksOrderPaid() throws Exception {
        var payload = event("payment_intent.succeeded", order.getId());

        var response = post(payload, sign(payload));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.Paid);
    }

    @Test
    void paymentFailed_withValidSignature_marksOrderFailed() throws Exception {
        var payload = event("payment_intent.payment_failed", order.getId());

        var response = post(payload, sign(payload));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // With orders in both currencies, the webhook updates only the order it
    // names and never touches the currency the order was charged in.
    @Test
    void paymentSucceeded_forEurOrder_marksOnlyThatOrderPaidAndKeepsItsCurrency() throws Exception {
        var eurOrder = new Order();
        eurOrder.setCustomer(order.getCustomer());
        eurOrder.setStatus(PaymentStatus.PENDING);
        eurOrder.setCurrency(SupportedCurrency.EUR);
        eurOrder.setTotalPrice(new BigDecimal("53.60"));
        eurOrder = orderRepository.save(eurOrder);
        var payload = event("payment_intent.succeeded", eurOrder.getId());

        var response = post(payload, sign(payload));

        assertThat(response.statusCode()).isEqualTo(200);
        var paid = orderRepository.findById(eurOrder.getId()).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.Paid);
        assertThat(paid.getCurrency()).isEqualTo(SupportedCurrency.EUR);
        assertThat(paid.getTotalPrice()).isEqualByComparingTo("53.60");
        // The other (DKK) order is untouched.
        assertThat(currentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getCurrency())
                .isEqualTo(SupportedCurrency.DKK);
    }

    @Test
    void webhook_withInvalidSignature_returns400AndLeavesOrderPending() throws Exception {
        var payload = event("payment_intent.succeeded", order.getId());

        var response = post(payload, "t=1,v1=deadbeef");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhook_withoutSignature_returns400AndLeavesOrderPending() throws Exception {
        var payload = event("payment_intent.succeeded", order.getId());

        var response = post(payload, null);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
