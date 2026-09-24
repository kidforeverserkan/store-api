package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.cart.Cart;
import com.kidforeverserkan.store.currency.CurrencyProperties;
import com.kidforeverserkan.store.currency.CurrencyService;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.products.Product;
import com.kidforeverserkan.store.users.User;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks exactly what would be sent to Stripe for an order in each currency,
 * without calling Stripe: the currency code and the per-unit amounts in
 * minor units, which must match what the order itself records.
 */
class StripeCheckoutCurrencyTests {

    private CurrencyService currencyService;
    private StripePaymentGateway gateway;
    private Cart cart;

    @BeforeEach
    void setUp() {
        var properties = new CurrencyProperties();
        properties.setDkkToEurRate(new BigDecimal("0.1340"));
        currencyService = new CurrencyService(properties);
        gateway = new StripePaymentGateway(currencyService);
        ReflectionTestUtils.setField(gateway, "websiteUrl", "http://localhost:8080");

        // 3 × Wireless Gaming Mouse (59.99 DKK) + 1 × 27-inch QHD Monitor (279.99 DKK)
        cart = new Cart();
        var mouse = product(1L, "Wireless Gaming Mouse", "59.99");
        cart.addItem(mouse);
        cart.addItem(mouse);
        cart.addItem(mouse);
        cart.addItem(product(3L, "27-inch QHD Monitor", "279.99"));
    }

    private Product product(Long id, String name, String dkkPrice) {
        var product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(dkkPrice));
        return product;
    }

    // Same construction checkout uses: convert each DKK unit price once.
    private Order orderIn(SupportedCurrency currency) {
        var order = Order.fromCart(cart, new User(), currency,
                price -> currencyService.convertFromBase(price, currency));
        order.setId(42L);
        return order;
    }

    private static long stripeTotal(SessionCreateParams params) {
        return params.getLineItems().stream()
                .mapToLong(li -> li.getPriceData().getUnitAmount() * li.getQuantity())
                .sum();
    }

    private static long unitAmountFor(SessionCreateParams params, String productName) {
        return params.getLineItems().stream()
                .filter(li -> li.getPriceData().getProductData().getName().equals(productName))
                .findFirst().orElseThrow()
                .getPriceData().getUnitAmount();
    }

    @Test
    void dkkOrder_isChargedInDkkAtTheCanonicalPrices() {
        var order = orderIn(SupportedCurrency.DKK);

        var params = gateway.buildSessionParams(order);

        assertThat(params.getLineItems())
                .allSatisfy(li -> assertThat(li.getPriceData().getCurrency()).isEqualTo("dkk"));
        assertThat(unitAmountFor(params, "Wireless Gaming Mouse")).isEqualTo(5999L);
        assertThat(unitAmountFor(params, "27-inch QHD Monitor")).isEqualTo(27999L);
        // 3 × 59.99 + 279.99 = 459.96 DKK — Stripe's total equals the order's.
        assertThat(stripeTotal(params)).isEqualTo(45996L);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("459.96");
    }

    @Test
    void eurOrder_isChargedInEurAtTheAmountsTheOrderRecorded() {
        var order = orderIn(SupportedCurrency.EUR);

        var params = gateway.buildSessionParams(order);

        assertThat(params.getLineItems())
                .allSatisfy(li -> assertThat(li.getPriceData().getCurrency()).isEqualTo("eur"));
        // 59.99 DKK -> €8.04, 279.99 DKK -> €37.52 (converted once, at order
        // creation). If the gateway converted again these would be €1.08/€5.03.
        assertThat(unitAmountFor(params, "Wireless Gaming Mouse")).isEqualTo(804L);
        assertThat(unitAmountFor(params, "27-inch QHD Monitor")).isEqualTo(3752L);
        // 3 × 8.04 + 37.52 = €61.64 — Stripe's total equals the order's.
        assertThat(stripeTotal(params)).isEqualTo(6164L);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("61.64");
    }

    @Test
    void checkoutSession_keepsOrderIdMetadataForTheWebhook() {
        var params = gateway.buildSessionParams(orderIn(SupportedCurrency.EUR));

        assertThat(params.getPaymentIntentData().getMetadata())
                .containsEntry("order_id", "42")
                .containsEntry("currency", "EUR");
        assertThat(params.getSuccessUrl()).isEqualTo("http://localhost:8080/checkout-success?orderId=42");
    }
}
