package com.kidforeverserkan.store.orders;

import com.kidforeverserkan.store.cart.Cart;
import com.kidforeverserkan.store.currency.SupportedCurrency;
import com.kidforeverserkan.store.payments.PaymentStatus;
import com.kidforeverserkan.store.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "created_at",insertable = false,updatable = false)
    private LocalDateTime createdAt;


    // Amounts below (and on the items) are in this currency.
    @Column(name = "total_price")
    private BigDecimal totalPrice;

    // The currency this order was charged in — fixed at checkout, never
    // the customer's current display preference. Defaults to DKK like
    // the column (V7), which is what every pre-multi-currency order was.
    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private SupportedCurrency currency = SupportedCurrency.BASE;

    @OneToMany(mappedBy = "order",cascade = {CascadeType.PERSIST,CascadeType.REMOVE})
    private Set<OrderItem> items = new LinkedHashSet<>();


    // Records what is actually charged: each DKK product price is
    // converted once, here, to the checkout currency, and the total is
    // the sum of the converted lines (the same amounts Stripe receives).
    public static Order fromCart(Cart cart, User customer, SupportedCurrency currency,
                                 UnaryOperator<BigDecimal> toOrderCurrency) {
        var order = new Order();
        order.setCustomer(customer);
        order.setStatus(PaymentStatus.PENDING);
        order.setCurrency(currency);

        cart.getItems().forEach(item -> {
            var unitPrice = toOrderCurrency.apply(item.getProduct().getPrice());
            var orderItem = new OrderItem(order, item.getProduct(), item.getQuantity(), unitPrice);
            order.items.add(orderItem);
        });

        order.setTotalPrice(order.items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return order;
    }

    public boolean isPlacedBy(User customer) {
        return this.customer.equals(customer);
    }


}