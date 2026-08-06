package com.codewithmosh.store.service;

import com.codewithmosh.store.dtos.CartDto;
import com.codewithmosh.store.dtos.CartItemDto;
import com.codewithmosh.store.entities.Cart;
import com.codewithmosh.store.exceptions.CartNotFoundException;
import com.codewithmosh.store.exceptions.ProductNotFoundException;
import com.codewithmosh.store.mappers.CartMapper;
import com.codewithmosh.store.repositories.CartRepository;
import com.codewithmosh.store.repositories.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.UUID;

@AllArgsConstructor
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final ProductRepository productRepository;


    public CartDto createCart() {

        var cart = new Cart();
        cartRepository.save(cart);

        return cartMapper.toCartDto(cart);
    }

    public CartItemDto addToCart(UUID cartId, Long productId) {
        var cart = cartRepository.getCartWhiteItems(cartId).orElseThrow(null);
        if (cart == null) {
            throw new CartNotFoundException();
        }

        var product = productRepository.findById(productId).orElseThrow(null);
        if (product == null) {
            throw new ProductNotFoundException();
        }

        var cartItem = cart.addItem(product);

        cartRepository.save(cart);

        return cartMapper.toDto(cartItem);
    }

    public CartDto getCart(UUID cartId) {
        var cart = cartRepository
                .getCartWhiteItems(cartId)
                .orElseThrow(CartNotFoundException::new);
        return cartMapper.toCartDto(cart);
    }

    public CartItemDto updateCart(UUID cartId, Long productId, Integer quantity) {
        var cart = cartRepository.getCartWhiteItems(cartId).orElseThrow(CartNotFoundException::new);
        var cartItem = cart.getItem(productId);
        if (cartItem == null) {
            throw new ProductNotFoundException();
        }
        cartItem.setQuantity(quantity);
        cartRepository.save(cart);
        return cartMapper.toDto(cartItem);

       }

    public void removeItemFromCart(UUID cartId, Long productId) {
        var cart = cartRepository.getCartWhiteItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        cart.removeItemFromCart(productId);

        cartRepository.save(cart);
    }

    public void clearCart(UUID cartId){
        var cart = cartRepository.getCartWhiteItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        cart.clear();
        cartRepository.save(cart);
    }
}

