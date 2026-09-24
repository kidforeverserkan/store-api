package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.orders.Order;

import java.util.Optional;

public interface PaymentGateway {
    // Charges order.getCurrency() using the order's recorded item prices.
    CheckoutSession createCheckoutSession (Order order);
    Optional<PaymentResult> parseWebhookEvent(WebhookRequest request);
}
