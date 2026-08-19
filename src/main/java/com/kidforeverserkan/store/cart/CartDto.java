package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Represents a shopping cart.")
@Data
public class CartDto {

    @Schema(
            description = "Unique identifier of the shopping cart."
    )
    private UUID id;

    @Schema(description = "Products currently in the shopping cart.")
    private List<CartItemDto> items;

    @Schema(
            description = "Total value of the shopping cart.",
            example = "60.00"
    )
    private BigDecimal totalPrice;
}
