package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Request for adding a product to a shopping cart.")
@Data
public class AddItemToCartRequest {

    @Schema(
            description = "Unique identifier of the product.",
            example = "1"
    )
    private Long productId;
}

