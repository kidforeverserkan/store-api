package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.orders.Order;

import java.util.Optional;

public interface PaymentGateway {
    CheckoutSession createCheckoutSession (Order order);
    Optional<PaymentResult> parseWebhookEvent(WebhookRequest request);
}
