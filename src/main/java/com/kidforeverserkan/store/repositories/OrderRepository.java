package com.kidforeverserkan.store.repositories;

import com.kidforeverserkan.store.entities.Order;
import com.kidforeverserkan.store.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = "items.product")
    @Query("SELECT O FROM  Order O WHERE O.customer = :customer")
    List<Order> getOrdersByCustomer(@Param("customer") User customer);



    @EntityGraph(attributePaths = "items.product")
    @Query("SELECT O FROM Order O WHERE O.id = :orderId")
    Optional<Order> getOrderWithItems(@Param("orderId") Long orderId);
}