package com.kidforeverserkan.store.currency;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

/**
 * Exposes the configured rates so the storefront converts prices with
 * exactly the same numbers checkout uses, instead of keeping its own copy.
 * Rates are sent as plain decimal strings so the browser can do exact
 * (integer) arithmetic without floating-point drift.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public CurrencyRatesDto getRates() {
        var rates = new LinkedHashMap<String, String>();
        currencyService.rates().forEach((currency, rate) ->
                rates.put(currency.name(), rate.toPlainString()));

        return new CurrencyRatesDto(
                SupportedCurrency.BASE.name(),
                rates,
                "Fixed demo exchange rate for this portfolio project, not live pricing."
        );
    }
}
