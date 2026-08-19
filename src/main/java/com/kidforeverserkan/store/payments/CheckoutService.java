package com.kidforeverserkan.store.payments;

import com.kidforeverserkan.store.orders.Order;
import com.kidforeverserkan.store.exceptions.CartEmptyException;
import com.kidforeverserkan.store.exceptions.CartNotFoundException;
import com.kidforeverserkan.store.cart.CartRepository;
import com.kidforeverserkan.store.orders.OrderRepository;
import com.kidforeverserkan.store.auth.AuthService;
import com.kidforeverserkan.store.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CheckoutService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final CartService cartService;
    private final PaymentGateway paymentGateway;


    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request){
        var cart =  cartRepository.getCartWhiteItems(request.getCartId()).orElse(null);
        if (cart == null) {
            throw new CartNotFoundException();
        }

        if (cart.isEmpty()){
            throw new CartEmptyException();
        }

        var order = Order.fromCart(cart,authService.getCurrentUser());

        orderRepository.save(order);

           try {
               var session = paymentGateway.createCheckoutSession(order);

               cartService.clearCart(cart.getId());

               return new CheckoutResponse(order.getId(),session.getCheckoutUrl());
           }

           catch (PaymentException ex) {
               System.out.println(ex.getMessage());
               orderRepository.delete(order);
               throw ex;
           }
    }

    public void handleWebhookEvent(WebhookRequest request){

        paymentGateway
                .parseWebhookEvent(request)
                .ifPresent(paymentResult -> {
                    var order = orderRepository.findById(paymentResult.getOrderId()).orElseThrow();
                    order.setStatus(paymentResult.getPaymentStatus());
                    orderRepository.save(order);
                });
    }
}
