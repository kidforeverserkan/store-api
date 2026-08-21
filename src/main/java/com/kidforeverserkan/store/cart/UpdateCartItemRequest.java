package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "Request for updating the quantity of a product in a shopping cart.")
@Data
public class UpdateCartItemRequest {

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Schema(
            description = "New quantity of the product.",
            example = "3"
    )
    private Integer quantity;
}