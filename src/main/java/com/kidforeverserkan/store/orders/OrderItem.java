package com.kidforeverserkan.store.orders;

import com.kidforeverserkan.store.products.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;


    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;


    @Column(name = "unit_price")
    private BigDecimal unitPrice;


    @Column(name = "quantity")
    private Integer quantity;


    @Column(name = "total_price")
    private BigDecimal totalPrice;


    // unitPrice is the price actually charged per unit, in the order's
    // currency (the product's DKK price converted at checkout).
    public OrderItem(Order order, Product product, Integer quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(new BigDecimal(quantity));
    }
}