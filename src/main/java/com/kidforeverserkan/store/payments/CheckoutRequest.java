package com.kidforeverserkan.store.payments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequest {
    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    // Optional; omitted means DKK (the store's base currency). Only the
    // exact codes below are accepted — anything else is a 400, never a guess.
    @Pattern(regexp = "DKK|EUR", message = "Currency must be DKK or EUR")
    private String currency;
}
