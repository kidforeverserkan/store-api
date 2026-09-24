package com.kidforeverserkan.store.currency;

import java.util.Locale;

/**
 * The only currencies the store accepts. Product prices are stored in the
 * base currency (DKK); every other currency is derived from it by
 * {@link CurrencyService}. Both currencies use two-decimal minor units
 * (øre / cent), which is what Stripe expects for dkk and eur.
 */
public enum SupportedCurrency {
    DKK,
    EUR;

    public static final SupportedCurrency BASE = DKK;

    // A missing value means "the default" (DKK). Anything that isn't an
    // exact supported code is rejected earlier by request validation, so
    // this never has to guess at user input.
    public static SupportedCurrency fromCodeOrDefault(String code) {
        return code == null ? BASE : valueOf(code);
    }

    public String stripeCode() {
        return name().toLowerCase(Locale.ROOT);
    }
}
