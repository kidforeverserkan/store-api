package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.cart.Cart;
import com.kidforeverserkan.store.cart.CartRepository;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.orders.OrderRepository;
import com.kidforeverserkan.store.products.Category;
import com.kidforeverserkan.store.products.CategoryRepository;
import com.kidforeverserkan.store.products.Product;
import com.kidforeverserkan.store.products.ProductRepository;
import com.kidforeverserkan.store.users.Role;
import com.kidforeverserkan.store.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutAndOrderTests extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Stripe is never called from tests; the real network call is replaced
    // with a stub so checkout can be verified end-to-end without a Stripe
    // account.
    @MockitoBean
    private PaymentGateway paymentGateway;

    private Product product;

    @BeforeEach
    void setUp() {
        var category = categoryRepository.save(new Category("Electronics"));

        product = new Product();
        product.setName("Wireless Mouse");
        product.setDescription("A comfortable wireless mouse");
        product.setPrice(new BigDecimal("27.99"));
        product.setCategory(category);
        product = productRepository.save(product);
    }

    private Cart createCartWithItem() {
        var cart = new Cart();
        cart.addItem(product);
        return cartRepository.save(cart);
    }

    @Test
    void checkout_withEmptyCart_returns400() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var emptyCart = cartRepository.save(new Cart());

        mockMvc.perform(post("/checkout")
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("{\"cartId\":\"" + emptyCart.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withUnknownCart_returns400() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(post("/checkout")
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("{\"cartId\":\"" + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withValidCart_createsOrderAndReturnsCheckoutUrl() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var cart = createCartWithItem();

        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("https://stripe.example.com/session/test"));

        mockMvc.perform(post("/checkout")
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("{\"cartId\":\"" + cart.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.example.com/session/test"))
                .andExpect(jsonPath("$.orderId").exists());
    }

    // Runs a checkout and returns the order that was persisted for it (as
    // reloaded from the database), plus the order handed to the gateway.
    private Order[] checkoutAndCapture(User user, String currencyJson) throws Exception {
        var cart = createCartWithItem();
        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("https://stripe.example.com/session/test"));

        var response = mockMvc.perform(post("/checkout")
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("{\"cartId\":\"" + cart.getId() + "\"" + currencyJson + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var orderId = objectMapper.readTree(response).get("orderId").asLong();
        var sentToGateway = ArgumentCaptor.forClass(Order.class);
        verify(paymentGateway).createCheckoutSession(sentToGateway.capture());

        var stored = orderRepository.getOrderWithItems(orderId).orElseThrow();
        return new Order[] {stored, sentToGateway.getValue()};
    }

    // Older clients (and anyone calling the API directly) that don't send a
    // currency keep getting DKK, the store's base currency.
    @Test
    void checkout_withoutCurrency_createsDkkOrder() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        var orders = checkoutAndCapture(user, "");

        assertThat(orders[0].getCurrency()).isEqualTo(SupportedCurrency.DKK);
        assertThat(orders[1].getCurrency()).isEqualTo(SupportedCurrency.DKK);
        assertThat(orders[0].getTotalPrice()).isEqualByComparingTo("27.99");
    }

    @Test
    void checkout_withDkk_recordsDkkOrderAtTheCanonicalPrice() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        var orders = checkoutAndCapture(user, ",\"currency\":\"DKK\"");
        var stored = orders[0];

        assertThat(stored.getCurrency()).isEqualTo(SupportedCurrency.DKK);
        assertThat(orders[1].getCurrency()).isEqualTo(SupportedCurrency.DKK);
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("27.99");
        assertThat(stored.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getUnitPrice()).isEqualByComparingTo("27.99"));
    }

    // The order records what is charged in EUR (27.99 DKK × 0.1340 =
    // 3.75066 -> €3.75), so it stays correct even if the rate changes later.
    @Test
    void checkout_withEur_recordsEurOrderWithChargedAmounts() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        var orders = checkoutAndCapture(user, ",\"currency\":\"EUR\"");
        var stored = orders[0];

        assertThat(stored.getCurrency()).isEqualTo(SupportedCurrency.EUR);
        assertThat(orders[1].getCurrency()).isEqualTo(SupportedCurrency.EUR);
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("3.75");
        assertThat(stored.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getUnitPrice()).isEqualByComparingTo("3.75");
                    assertThat(item.getTotalPrice()).isEqualByComparingTo("3.75");
                });
        // The catalog price itself is untouched (still canonical DKK).
        assertThat(productRepository.findById(product.getId()).orElseThrow().getPrice())
                .isEqualByComparingTo("27.99");
    }

    // Only exact DKK/EUR codes are trusted; anything else is rejected before
    // an order is created or Stripe is contacted.
    @Test
    void checkout_withUnsupportedCurrency_returns400AndCreatesNothing() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var cart = createCartWithItem();
        var ordersBefore = orderRepository.count();

        for (var currency : new String[] {"USD", "eur", "", "DKK;EUR"}) {
            mockMvc.perform(post("/checkout")
                            .header("Authorization", bearerToken(user))
                            .contentType("application/json")
                            .content("{\"cartId\":\"" + cart.getId() + "\",\"currency\":\"" + currency + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.currency").value("Currency must be DKK or EUR"));
        }

        verify(paymentGateway, never()).createCheckoutSession(any());
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
        assertThat(cartRepository.findById(cart.getId()).orElseThrow().getItems()).hasSize(1);
    }

    @Test
    void getOrder_asOwner_returnsOrder() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var order = createOrderFor(user);

        mockMvc.perform(get("/orders/{id}", order.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()));
    }

    // The API exposes each order's own currency next to amounts that are
    // already in that currency — the client never has to convert them.
    @Test
    void getOrder_exposesTheCurrencyTheOrderWasChargedIn() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var dkkOrder = createOrderFor(user);
        var eurOrder = orderRepository.save(Order.fromCart(createCartWithItem(), user,
                SupportedCurrency.EUR, price -> new BigDecimal("3.75")));

        mockMvc.perform(get("/orders/{id}", dkkOrder.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("DKK"))
                .andExpect(jsonPath("$.totalPrice").value(27.99));

        mockMvc.perform(get("/orders/{id}", eurOrder.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.totalPrice").value(3.75))
                .andExpect(jsonPath("$.items[0].price").value(3.75));

        mockMvc.perform(get("/orders")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].currency", org.hamcrest.Matchers.containsInAnyOrder("DKK", "EUR")));
    }

    @Test
    void getOrder_asOtherUser_returnsForbidden() throws Exception {
        var owner = createUser("owner@example.com", "password123", Role.USER);
        var otherUser = createUser("other@example.com", "password123", Role.USER);
        var order = createOrderFor(owner);

        mockMvc.perform(get("/orders/{id}", order.getId())
                        .header("Authorization", bearerToken(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_returnsOnlyOwnOrders() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var otherUser = createUser("other@example.com", "password123", Role.USER);
        createOrderFor(user);
        createOrderFor(otherUser);

        mockMvc.perform(get("/orders")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private Order createOrderFor(User user) {
        var cart = createCartWithItem();
        var order = Order.fromCart(cart, user, SupportedCurrency.DKK, price -> price);
        return orderRepository.save(order);
    }
}
