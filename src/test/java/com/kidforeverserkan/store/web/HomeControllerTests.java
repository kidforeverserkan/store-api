package com.kidforeverserkan.store.web;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTests extends AbstractIntegrationTest {

    // Regression test: "/" was not in SecurityConfig's permitAll list, so the
    // public landing page returned 401 instead of rendering for anonymous
    // visitors.
    @Test
    void landingPage_withoutToken_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
