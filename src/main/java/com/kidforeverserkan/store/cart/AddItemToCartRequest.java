package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "Request for adding a product to a shopping cart.")
@Data
public class AddItemToCartRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    @Schema(
            description = "Unique identifier of the product.",
            example = "1"
    )
    private Long productId;
}