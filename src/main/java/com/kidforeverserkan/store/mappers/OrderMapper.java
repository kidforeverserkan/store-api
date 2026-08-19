package com.kidforeverserkan.store.mappers;

import com.kidforeverserkan.store.dtos.OrderDto;
import com.kidforeverserkan.store.entities.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderDto toDto(Order order);
}
