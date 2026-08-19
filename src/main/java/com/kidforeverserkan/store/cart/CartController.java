package com.kidforeverserkan.store.cart;

import com.kidforeverserkan.store.exceptions.CartNotFoundException;
import com.kidforeverserkan.store.exceptions.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/carts")
@Tag(
        name = "Shopping Cart",
        description = "Operations for creating and managing shopping carts."
)
public class CartController {

    private final CartService cartService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Create a shopping cart",
            description = "Creates a new empty shopping cart and returns its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Shopping cart created successfully")
    })
    @PostMapping
    public ResponseEntity<CartDto> createCart(UriComponentsBuilder uriBuilder) {

        var cartDto = cartService.createCart();

        var uri = uriBuilder
                .path("/carts/{id}")
                .buildAndExpand(cartDto.getId())
                .toUri();

        return ResponseEntity.created(uri).body(cartDto);
    }

    // -------------------------------------------------------------------------
    // Add Product
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Add a product to a cart",
            description = "Adds one quantity of the specified product to the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product added successfully"),
            @ApiResponse(responseCode = "400", description = "Product not found"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartItemDto> addToCart(

            @Parameter(description = "Unique identifier of the shopping cart.")
            @PathVariable UUID cartId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "The product to add to the shopping cart."
            )
            @RequestBody AddItemToCartRequest request) {

        var cartItemDto =
                cartService.addToCart(cartId, request.getProductId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartItemDto);
    }

    // -------------------------------------------------------------------------
    // Get Cart
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Retrieve a shopping cart",
            description = "Returns a shopping cart including all products and the total price."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shopping cart retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}")
    public ResponseEntity<CartDto> getCart(

            @Parameter(description = "Unique identifier of the shopping cart.")
            @PathVariable UUID cartId) {

        var cartDto = cartService.getCart(cartId);

        return ResponseEntity.ok(cartDto);
    }

    // -------------------------------------------------------------------------
    // Update Product Quantity
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Update product quantity",
            description = "Updates the quantity of a product already in the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Product not found"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @PutMapping("/{cartId}/items/{productId}")
    public ResponseEntity<CartItemDto> updateCart(

            @Parameter(description = "Unique identifier of the shopping cart.")
            @PathVariable UUID cartId,

            @Parameter(description = "Unique identifier of the product.")
            @PathVariable Long productId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "The new quantity for the product."
            )
            @Valid
            @RequestBody UpdateCartItemRequest request) {

        var cartItemDto =
                cartService.updateCart(cartId, productId, request.getQuantity());

        return ResponseEntity.ok(cartItemDto);
    }

    // -------------------------------------------------------------------------
    // Remove Product
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Remove a product from a cart",
            description = "Removes the specified product from the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product removed successfully"),
            @ApiResponse(responseCode = "400", description = "Product not found"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/{cartId}/items/{productId}")
    public ResponseEntity<Void> removeItemFromCart(

            @Parameter(description = "Unique identifier of the shopping cart.")
            @PathVariable UUID cartId,

            @Parameter(description = "Unique identifier of the product.")
            @PathVariable Long productId) {

        cartService.removeItemFromCart(cartId, productId);

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Clear Cart
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Clear a shopping cart",
            description = "Removes all products from the shopping cart."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shopping cart cleared successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<Void> clearCart(

            @Parameter(description = "Unique identifier of the shopping cart.")
            @PathVariable UUID cartId) {

        cartService.clearCart(cartId);

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Exception Handlers
    // -------------------------------------------------------------------------

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCartNotFound() {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Cart not found."));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound() {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Product not found."));
    }
}
