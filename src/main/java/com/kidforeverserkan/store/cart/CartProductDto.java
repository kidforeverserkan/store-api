package com.kidforeverserkan.store.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Basic information about a product in the shopping cart.")
@Data
public class CartProductDto {

    @Schema(
            description = "Unique identifier of the product.",
            example = "1"
    )
    private Long id;

    @Schema(
            description = "Product name.",
            example = "Test Product 1"
    )
    private String name;

    @Schema(
            description = "Price of a single product.",
            example = "20.00"
    )
    private BigDecimal price;
}
