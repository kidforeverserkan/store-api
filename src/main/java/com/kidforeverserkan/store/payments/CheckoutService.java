package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.exceptions.CartEmptyException;
import com.kidforeverserkan.store.exceptions.CartNotFoundException;
import com.kidforeverserkan.store.cart.CartRepository;
import com.kidforeverserkan.store.orders.OrderRepository;
import com.kidforeverserkan.store.auth.AuthService;
import com.kidforeverserkan.store.cart.CartService;
import com.kidforeverserkan.store.currency.CurrencyService;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final CartService cartService;
    private final PaymentGateway paymentGateway;
    private final CurrencyService currencyService;


    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request){
        var cart =  cartRepository.getCartWithItems(request.getCartId()).orElse(null);
        if (cart == null) {
            throw new CartNotFoundException();
        }

        if (cart.isEmpty()){
            throw new CartEmptyException();
        }

        // Already validated as DKK/EUR (or omitted -> DKK) by CheckoutRequest.
        var currency = SupportedCurrency.fromCodeOrDefault(request.getCurrency());
        var order = Order.fromCart(cart, authService.getCurrentUser(), currency,
                price -> currencyService.convertFromBase(price, currency));

        orderRepository.save(order);

           try {
               // The gateway charges the order's own currency and amounts, so
               // what Stripe charges and what the order records always agree.
               var session = paymentGateway.createCheckoutSession(order);

               cartService.clearCart(cart.getId());

               return new CheckoutResponse(order.getId(),session.getCheckoutUrl());
           }

           catch (PaymentException ex) {
               log.error("Checkout failed for cart {}: {}", request.getCartId(), ex.getMessage());
               orderRepository.delete(order);
               throw ex;
           }
    }

    public void handleWebhookEvent(WebhookRequest request){

        paymentGateway
                .parseWebhookEvent(request)
                .ifPresent(paymentResult -> {
                    var order = orderRepository.findById(paymentResult.getOrderId()).orElseThrow();
                    // Stripe delivers events at least once and in no guaranteed
                    // order, so a redelivered or late event must never undo a
                    // confirmed payment. It is still acknowledged (200).
                    if (order.getStatus() == PaymentStatus.Paid) {
                        return;
                    }
                    order.setStatus(paymentResult.getPaymentStatus());
                    orderRepository.save(order);
                });
    }
}
