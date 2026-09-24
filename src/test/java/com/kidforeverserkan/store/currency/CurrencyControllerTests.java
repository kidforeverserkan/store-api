package com.kidforeverserkan.store.currency;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CurrencyControllerTests extends AbstractIntegrationTest {

    // The storefront loads these before showing any price, so they must be
    // readable without logging in.
    @Test
    void rates_arePublicAndComeFromConfiguration() throws Exception {
        mockMvc.perform(get("/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("DKK"))
                .andExpect(jsonPath("$.rates.DKK").value("1"))
                // YAML reads 0.1340 as the number 0.134 — same rate.
                .andExpect(jsonPath("$.rates.EUR").value("0.134"))
                .andExpect(jsonPath("$.notice").exists());
    }

    @Test
    void rates_areReadOnly() throws Exception {
        mockMvc.perform(post("/currencies"))
                .andExpect(status().isUnauthorized());
    }
}
