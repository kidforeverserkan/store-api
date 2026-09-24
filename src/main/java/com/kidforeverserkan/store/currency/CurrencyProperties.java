package com.kidforeverserkan.store.currency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Fixed, configurable exchange rate for the storefront's EUR prices.
 *
 * This is a portfolio/demo rate set in application.yaml
 * (store.currency.dkk-to-eur-rate), NOT a live financial pricing feed: it
 * only changes when someone edits the configuration. The app fails to start
 * if it is missing or not positive.
 */
@Configuration
@ConfigurationProperties(prefix = "store.currency")
@Validated
@Data
public class CurrencyProperties {

    /** How many EUR one DKK is worth, e.g. 0.1340. */
    @NotNull
    @Positive
    private BigDecimal dkkToEurRate;
}
