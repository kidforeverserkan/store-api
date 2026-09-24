package com.kidforeverserkan.store.admin;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.users.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminEndpointTests extends AbstractIntegrationTest {

    @Test
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_asRegularUser_returnsForbidden() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/admin/hello")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_asAdmin_returnsOk() throws Exception {
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(get("/admin/hello")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk());
    }

    // Regression test: UserController's own role/ownership checks throw
    // AccessDeniedException, which the catch-all handler used to turn into
    // a 500 instead of a 403.
    @Test
    void listUsers_asRegularUser_returnsForbiddenNotServerError() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/users")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOtherUser_asRegularUser_returnsForbiddenNotServerError() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var other = createUser("other@example.com", "password123", Role.USER);

        mockMvc.perform(get("/users/{id}", other.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());
    }

    // Regression test: an unknown path raised NoResourceFoundException,
    // which the catch-all handler used to report as a 500 instead of a 404.
    @Test
    void unknownPath_whenAuthenticated_returns404NotServerError() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/does-not-exist")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNotFound());
    }
}
