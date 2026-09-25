package com.kidforeverserkan.store.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidforeverserkan.store.auth.JwtService;
import com.kidforeverserkan.store.cart.Cart;
import com.kidforeverserkan.store.cart.CartRepository;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.orders.OrderRepository;
import com.kidforeverserkan.store.products.Category;
import com.kidforeverserkan.store.products.Product;
import com.kidforeverserkan.store.products.ProductRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // The real gateway, so webhook signature verification and event parsing
    // run for real. Only creating the Stripe checkout session (a network
    // call) is stubbed, in the test that goes through checkout.
    @MockitoSpyBean
    private StripePaymentGateway paymentGateway;

    private final HttpClient http = HttpClient.newHttpClient();

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setCustomer(saveCustomer("Webhook Customer"));
        order.setStatus(PaymentStatus.PENDING);
        order.setTotalPrice(new BigDecimal("10.00"));
        order = orderRepository.save(order);
    }

    private User saveCustomer(String name) {
        var user = new User();
        user.setName(name);
        user.setEmail("webhook-" + UUID.randomUUID() + "@example.com");
        user.setPassword("not-used");
        user.setRole(Role.USER);
        return userRepository.save(user);
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

    // Stripe delivers webhooks at least once, so the same payment
    // confirmation can arrive twice. The order is created at checkout; the
    // confirmation only settles it. Replaying the confirmation must leave the
    // customer with the one order they placed (still Paid, same items and
    // amounts) and be acknowledged with 200 so Stripe stops retrying.
    @Test
    void replayedPaymentConfirmation_leavesCustomerWithExactlyOnePaidOrder() throws Exception {
        var customer = saveCustomer("Replay Customer");
        var product = saveProduct("Mechanical Keyboard", "89.50");
        var cart = cartRepository.save(new Cart());
        cart.addItem(product);
        cart.addItem(product);
        cart = cartRepository.save(cart);
        doReturn(new CheckoutSession("https://stripe.example.com/session/test"))
                .when(paymentGateway).createCheckoutSession(any());
        var ordersBefore = orderRepository.count();

        var checkout = postCheckout(customer, cart);
        assertThat(checkout.statusCode()).isEqualTo(200);
        var orderId = objectMapper.readTree(checkout.body()).get("orderId").asLong();
        assertThat(orderRepository.getOrdersByCustomer(customer)).singleElement()
                .satisfies(o -> assertThat(o.getStatus()).isEqualTo(PaymentStatus.PENDING));

        // The exact same delivery (same bytes, same signature), twice.
        var payload = event("payment_intent.succeeded", orderId);
        var signature = sign(payload);

        var first = post(payload, signature);

        assertThat(first.statusCode()).isEqualTo(200);
        var confirmed = orderRepository.getOrdersByCustomer(customer);
        assertThat(confirmed).singleElement().satisfies(o -> {
            assertThat(o.getId()).isEqualTo(orderId);
            assertThat(o.getStatus()).isEqualTo(PaymentStatus.Paid);
        });
        var original = confirmed.get(0);

        var replay = post(payload, signature);

        assertThat(replay.statusCode()).isEqualTo(200);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore + 1);
        assertThat(orderRepository.getOrdersByCustomer(customer)).singleElement().satisfies(o -> {
            assertThat(o.getId()).isEqualTo(orderId);
            assertThat(o.getStatus()).isEqualTo(PaymentStatus.Paid);
            assertThat(o.getCurrency()).isEqualTo(SupportedCurrency.DKK);
            assertThat(o.getTotalPrice()).isEqualByComparingTo("179.00");
            assertThat(o.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(o.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getProduct().getId()).isEqualTo(product.getId());
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getUnitPrice()).isEqualByComparingTo("89.50");
                assertThat(item.getTotalPrice()).isEqualByComparingTo("179.00");
            });
        });
    }

    // Stripe also doesn't guarantee delivery order: a retried failure from an
    // earlier declined attempt can land after the payment went through. A
    // paid order must stay paid.
    @Test
    void lateFailureEvent_afterPaymentSucceeded_leavesOrderPaid() throws Exception {
        var succeeded = event("payment_intent.succeeded", order.getId());
        assertThat(post(succeeded, sign(succeeded)).statusCode()).isEqualTo(200);

        var failed = event("payment_intent.payment_failed", order.getId());
        var response = post(failed, sign(failed));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.Paid);
    }

    // The reverse is a normal retry (card declined, then another card
    // works) and must still settle the order as paid.
    @Test
    void paymentSucceeded_afterEarlierFailure_marksOrderPaid() throws Exception {
        var failed = event("payment_intent.payment_failed", order.getId());
        assertThat(post(failed, sign(failed)).statusCode()).isEqualTo(200);

        var succeeded = event("payment_intent.succeeded", order.getId());
        var response = post(succeeded, sign(succeeded));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(currentStatus()).isEqualTo(PaymentStatus.Paid);
    }

    private Product saveProduct(String name, String price) {
        var product = new Product();
        product.setName(name);
        product.setDescription(name);
        product.setPrice(new BigDecimal(price));
        product.setCategory(new Category("Webhook Test"));
        return productRepository.save(product);
    }

    private HttpResponse<String> postCheckout(User customer, Cart cart) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/checkout"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtService.generateAccessToken(customer))
                .POST(HttpRequest.BodyPublishers.ofString("{\"cartId\":\"" + cart.getId() + "\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
