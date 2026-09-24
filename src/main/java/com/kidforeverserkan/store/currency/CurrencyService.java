package com.kidforeverserkan.store.currency;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The one place prices are converted out of the base currency (DKK).
 *
 * Rounding rule, used identically by the storefront (static/js/currency.js)
 * and by Stripe checkout: a DKK *unit* price is converted and rounded
 * half-up to 2 decimals; line and cart totals are then unit × quantity,
 * summed. Stripe computes its totals the same way from the per-unit
 * amounts it's given, so the site and Stripe always show the same numbers.
 */
@AllArgsConstructor
@Service
public class CurrencyService {

    private static final int MINOR_UNIT_DIGITS = 2;

    private final CurrencyProperties properties;

    public BigDecimal rateFromBase(SupportedCurrency currency) {
        return switch (currency) {
            case DKK -> BigDecimal.ONE;
            case EUR -> properties.getDkkToEurRate();
        };
    }

    public BigDecimal convertFromBase(BigDecimal baseAmount, SupportedCurrency currency) {
        return baseAmount
                .multiply(rateFromBase(currency))
                .setScale(MINOR_UNIT_DIGITS, RoundingMode.HALF_UP);
    }

    // Stripe amounts are integers in the currency's minor unit
    // (øre for DKK, cents for EUR).
    public long toMinorUnits(BigDecimal amount) {
        return amount
                .setScale(MINOR_UNIT_DIGITS, RoundingMode.HALF_UP)
                .movePointRight(MINOR_UNIT_DIGITS)
                .longValueExact();
    }

    public Map<SupportedCurrency, BigDecimal> rates() {
        var rates = new LinkedHashMap<SupportedCurrency, BigDecimal>();
        for (var currency : SupportedCurrency.values()) {
            rates.put(currency, rateFromBase(currency));
        }
        return rates;
    }
}
