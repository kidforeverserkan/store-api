package com.kidforeverserkan.store.orders;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderDto toDto(Order order);

    @Mapping(source = "unitPrice", target = "price")
    OrderItemDto toDto(OrderItem orderItem);
}