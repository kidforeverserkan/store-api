package com.kidforeverserkan.store.cart;

import com.kidforeverserkan.store.exceptions.CartNotFoundException;
import com.kidforeverserkan.store.exceptions.ProductNotFoundException;
import com.kidforeverserkan.store.products.ProductRepository;
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

        var cart = cartRepository
                .getCartWithItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        var product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        var cartItem = cart.addItem(product);

        cartRepository.save(cart);

        return cartMapper.toDto(cartItem);
    }

    public CartDto getCart(UUID cartId) {

        var cart = cartRepository
                .getCartWithItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        return cartMapper.toCartDto(cart);
    }

    public CartItemDto updateCart(
            UUID cartId,
            Long productId,
            Integer quantity
    ) {

        var cart = cartRepository
                .getCartWithItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        var cartItem = cart.getItem(productId);

        if (cartItem == null) {
            throw new ProductNotFoundException();
        }

        cartItem.setQuantity(quantity);

        cartRepository.save(cart);

        return cartMapper.toDto(cartItem);
    }

    public void removeItemFromCart(
            UUID cartId,
            Long productId
    ) {

        var cart = cartRepository
                .getCartWithItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        cart.removeItemFromCart(productId);

        cartRepository.save(cart);
    }

    public void clearCart(UUID cartId) {

        var cart = cartRepository
                .getCartWithItems(cartId)
                .orElseThrow(CartNotFoundException::new);

        cart.clear();

        cartRepository.save(cart);
    }
}