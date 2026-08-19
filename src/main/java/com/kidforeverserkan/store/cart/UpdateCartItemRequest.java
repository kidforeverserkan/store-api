package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Request for updating the quantity of a product in a shopping cart.")
@Data
public class UpdateCartItemRequest {

    @Schema(
            description = "New quantity of the product.",
            example = "3"
    )
    private Integer quantity;
}
