package com.kidforeverserkan.store.web;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.users.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The header's "Server online/offline" pill calls GET /actuator/health from
// every page, logged in or not. It must stay public and reveal nothing but
// the overall status.
class HealthEndpointTests extends AbstractIntegrationTest {

    @Test
    void health_isPublicAndReportsOnlyTheStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void otherActuatorEndpoints_areNotExposed() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        for (var path : new String[] {"/actuator/env", "/actuator/beans", "/actuator/configprops", "/actuator/info"}) {
            mockMvc.perform(get(path).header("Authorization", bearerToken(admin)))
                    .andExpect(status().isNotFound());
        }
    }
}
