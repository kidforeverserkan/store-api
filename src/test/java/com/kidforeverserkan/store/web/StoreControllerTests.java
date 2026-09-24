package com.kidforeverserkan.store.web;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StoreControllerTests extends AbstractIntegrationTest {

    @Test
    void allStorefrontPages_arePubliclyAccessible() throws Exception {
        for (String path : new String[] {
                "/shop", "/shop/1", "/cart", "/login", "/register",
                "/checkout-success", "/checkout-cancel", "/orders"
        }) {
            mockMvc.perform(get(path).accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        }
    }

    @Test
    void aboutAndContactPages_arePublicAndUseTheSharedLayout() throws Exception {
        for (String path : new String[] {"/about", "/contact"}) {
            mockMvc.perform(get(path).accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    // Shared header/footer fragments rendered into the page.
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"site-header\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"site-footer\"")));
        }
    }

    // Regression test: StoreController's "/orders" page route and
    // OrderController's "/orders" API endpoint originally collided on the
    // exact same path with no way to disambiguate, which failed application
    // startup entirely ("Ambiguous mapping"). Both now declare an explicit
    // `produces`, split by the request's Accept header.
    @Test
    void ordersPath_servesHtmlOrJsonDependingOnAcceptHeader() throws Exception {
        mockMvc.perform(get("/orders").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        // No token: the JSON endpoint is still auth-protected, unlike the
        // page shell around it, so this correctly reaches the real API
        // rather than the page.
        mockMvc.perform(get("/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
