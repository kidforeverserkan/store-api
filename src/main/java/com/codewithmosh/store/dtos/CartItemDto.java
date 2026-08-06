package com.codewithmosh.store.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Represents a single item inside a shopping cart.")
@Data
public class CartItemDto {

    @Schema(description = "Product information.")
    private CartProductDto product;

    @Schema(
            description = "Quantity of the product.",
            example = "3"
    )
    private Integer quantity;

    @Schema(
            description = "Total price for this cart item.",
            example = "60.00"
    )
    private BigDecimal totalPrice;
}
