package com.kidforeverserkan.store.currency;

import java.util.Map;

public record CurrencyRatesDto(String base, Map<String, String> rates, String notice) {
}
