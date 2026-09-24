package com.kidforeverserkan.store.currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyServiceTests {

    private CurrencyService currencyService;

    @BeforeEach
    void setUp() {
        var properties = new CurrencyProperties();
        properties.setDkkToEurRate(new BigDecimal("0.1340"));
        currencyService = new CurrencyService(properties);
    }

    @Test
    void dkk_isTheBaseCurrencyAndTheDefault() {
        assertThat(SupportedCurrency.BASE).isEqualTo(SupportedCurrency.DKK);
        assertThat(SupportedCurrency.fromCodeOrDefault(null)).isEqualTo(SupportedCurrency.DKK);
    }

    @Test
    void supportedCodes_parse() {
        assertThat(SupportedCurrency.fromCodeOrDefault("DKK")).isEqualTo(SupportedCurrency.DKK);
        assertThat(SupportedCurrency.fromCodeOrDefault("EUR")).isEqualTo(SupportedCurrency.EUR);
    }

    @Test
    void unsupportedCode_isRejectedNotGuessed() {
        assertThatThrownBy(() -> SupportedCurrency.fromCodeOrDefault("USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripeCodes_areLowercaseIso() {
        assertThat(SupportedCurrency.DKK.stripeCode()).isEqualTo("dkk");
        assertThat(SupportedCurrency.EUR.stripeCode()).isEqualTo("eur");
    }

    @Test
    void convertingToDkk_keepsTheCanonicalPrice() {
        assertThat(currencyService.convertFromBase(new BigDecimal("59.99"), SupportedCurrency.DKK))
                .isEqualByComparingTo("59.99");
    }

    @Test
    void convertingToEur_appliesTheConfiguredRateAndRoundsToCents() {
        // 59.99 × 0.1340 = 8.03866 -> 8.04
        assertThat(currencyService.convertFromBase(new BigDecimal("59.99"), SupportedCurrency.EUR))
                .isEqualByComparingTo("8.04");
        // 279.99 × 0.1340 = 37.51866 -> 37.52
        assertThat(currencyService.convertFromBase(new BigDecimal("279.99"), SupportedCurrency.EUR))
                .isEqualByComparingTo("37.52");
    }

    @Test
    void convertingToEur_roundsHalfUp() {
        // 12.50 × 0.1340 = 1.675 exactly -> 1.68 (half-up)
        assertThat(currencyService.convertFromBase(new BigDecimal("12.50"), SupportedCurrency.EUR))
                .isEqualByComparingTo("1.68");
    }

    @Test
    void minorUnits_areWholeOreOrCents() {
        assertThat(currencyService.toMinorUnits(new BigDecimal("59.99"))).isEqualTo(5999L);
        assertThat(currencyService.toMinorUnits(new BigDecimal("8.04"))).isEqualTo(804L);
        assertThat(currencyService.toMinorUnits(new BigDecimal("14.9"))).isEqualTo(1490L);
    }

    @Test
    void rates_listEveryCurrencyWithDkkAsOne() {
        assertThat(currencyService.rates())
                .containsOnlyKeys(SupportedCurrency.DKK, SupportedCurrency.EUR);
        assertThat(currencyService.rates().get(SupportedCurrency.DKK)).isEqualByComparingTo("1");
        assertThat(currencyService.rates().get(SupportedCurrency.EUR)).isEqualByComparingTo("0.1340");
    }
}
