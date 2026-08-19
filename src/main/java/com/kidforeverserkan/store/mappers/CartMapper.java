package com.kidforeverserkan.store.mappers;

import com.kidforeverserkan.store.dtos.CartDto;
import com.kidforeverserkan.store.dtos.CartItemDto;
import com.kidforeverserkan.store.entities.Cart;
import com.kidforeverserkan.store.entities.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(target = "totalPrice",expression = "java(cart.getTotalPrice())")
    CartDto toCartDto(Cart cart);

    @Mapping(target = "totalPrice", expression = "java(cartItem.getTotalPrice())")
    CartItemDto toDto(CartItem cartItem);
}
