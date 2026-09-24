package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.currency.CurrencyService;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.orders.OrderItem;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    @Value("${websiteUrl}")
    private String websiteUrl;

    @Value("${stripe.webhookSecretKey}")
    private String webhookSecretKey;

    private final CurrencyService currencyService;

    public StripePaymentGateway(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Override
    public CheckoutSession createCheckoutSession(Order order) {

        try {
            var session = Session.create(buildSessionParams(order));

            return new CheckoutSession(session.getUrl());

        } catch (StripeException ex) {
            log.error("Stripe checkout session creation failed: {}", ex.getMessage());
            throw new PaymentException();
        }
    }

    @Override
    public Optional<PaymentResult> parseWebhookEvent(WebhookRequest request) {

        try {
            var payload = request.getPayload();
            var signature = request.getHeaders().get("stripe-signature");
            if (signature == null) {
                throw new WebhookVerificationException("Missing Stripe signature");
            }

            var event = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecretKey
            );

            return switch (event.getType()) {

                case "payment_intent.succeeded" ->
                        Optional.of(
                                new PaymentResult(
                                        extractOrderId(event),
                                        PaymentStatus.Paid
                                )
                        );

                case "payment_intent.payment_failed" ->
                        Optional.of(
                                new PaymentResult(
                                        extractOrderId(event),
                                        PaymentStatus.FAILED
                                )
                        );

                default ->
                        Optional.empty();
            };

        } catch (SignatureVerificationException e) {
            throw new WebhookVerificationException("Invalid Stripe signature");
        }
    }

    private Long extractOrderId(Event event) {

        var stripeObject =
                event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(
                                () -> new PaymentException(
                                        "could not deserialize Stripe event. " +
                                                "Check the SDK and API version."
                                )
                        );

        var paymentIntent = (PaymentIntent) stripeObject;

        return Long.valueOf(
                paymentIntent
                        .getMetadata()
                        .get("order_id")
        );
    }

    // Package-private so tests can check exactly what Stripe would receive
    // (currency + per-unit amounts) without calling Stripe.
    SessionCreateParams buildSessionParams(Order order) {
        var currency = order.getCurrency();
        var builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(
                        websiteUrl + "/checkout-success?orderId=" + order.getId()
                )
                .setCancelUrl(
                        websiteUrl + "/checkout-cancel"
                )
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata(
                                        "order_id",
                                        order.getId().toString()
                                )
                                .putMetadata(
                                        "currency",
                                        currency.name()
                                )
                                .build()
                );

        order.getItems().forEach(item -> builder.addLineItem(createLineItem(item, currency)));

        return builder.build();
    }

    private SessionCreateParams.LineItem createLineItem(OrderItem item, SupportedCurrency currency) {

        return SessionCreateParams.LineItem.builder()
                .setQuantity(Long.valueOf(item.getQuantity()))
                .setPriceData(createPriceData(item, currency))
                .build();
    }

    // Unit price only: Stripe multiplies by quantity itself, which matches
    // how the order's line totals were recorded. The unit price is already
    // in the order's currency (converted once, at checkout, by
    // Order.fromCart) — it is sent as-is, never converted again.
    private SessionCreateParams.LineItem.PriceData createPriceData(OrderItem item, SupportedCurrency currency) {
        return SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(currency.stripeCode())
                .setUnitAmount(currencyService.toMinorUnits(item.getUnitPrice()))
                .setProductData(createProductData(item))
                .build();
    }

    private SessionCreateParams.LineItem.PriceData.ProductData createProductData(
            OrderItem item
    ) {

        return SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName(item.getProduct().getName())
                .build();
    }
}