package com.kidforeverserkan.store.users;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The demo account email is configured in src/test/resources/application.yaml
// (store.demo.account-email), the same way DEMO_ACCOUNT_EMAIL sets it for
// the running app.
class DemoAccountProtectionTests extends AbstractIntegrationTest {

    private static final String DEMO_EMAIL = "demo@storeapi-demo.com";
    private static final String DEMO_MESSAGE = "The public demo account can't be changed or deleted.";

    private static final String UPDATE_BODY = """
            {"name":"Renamed","email":"renamed@example.com"}
            """;

    private static final String CHANGE_PASSWORD_BODY = """
            {"oldPassword":"password123","newPassword":"newPassword456"}
            """;

    // ---------------------------------------------------------- demo account

    @Test
    void demoUser_cannotUpdateItself() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);

        mockMvc.perform(put("/users/{id}", demo.getId())
                        .header("Authorization", bearerToken(demo))
                        .contentType("application/json")
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(DEMO_MESSAGE));

        var stored = userRepository.findById(demo.getId()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo(DEMO_EMAIL);
        assertThat(stored.getName()).isEqualTo("Test User");
    }

    @Test
    void demoUser_cannotDeleteItself() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);

        mockMvc.perform(delete("/users/{id}", demo.getId())
                        .header("Authorization", bearerToken(demo)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(DEMO_MESSAGE));

        assertThat(userRepository.existsById(demo.getId())).isTrue();
    }

    @Test
    void demoUser_cannotChangeItsPassword() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);

        mockMvc.perform(post("/users/{id}/change-password", demo.getId())
                        .header("Authorization", bearerToken(demo))
                        .contentType("application/json")
                        .content(CHANGE_PASSWORD_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(DEMO_MESSAGE));

        var stored = userRepository.findById(demo.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("password123", stored.getPassword())).isTrue();
    }

    @Test
    void demoUser_canStillReadItsOwnAccount() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);

        mockMvc.perform(get("/users/{id}", demo.getId())
                        .header("Authorization", bearerToken(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(DEMO_EMAIL));
    }

    // Admin behaviour is unchanged: an admin can still manage the demo account.
    @Test
    void admin_canStillUpdateTheDemoAccount() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);
        var admin = createUser("admin@example.com", "password123", Role.ADMIN);

        mockMvc.perform(put("/users/{id}", demo.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content("""
                                {"name":"Store API Demo Customer","email":"demo@storeapi-demo.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Store API Demo Customer"));
    }

    // ----------------------------------------------- normal accounts unchanged

    @Test
    void normalUser_canStillUpdateItself() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(put("/users/{id}", user.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.email").value("renamed@example.com"));
    }

    // Regression: blank or missing fields used to pass validation and then
    // fail on the NOT NULL columns (409); they are now a normal 400.
    @Test
    void updateUser_withBlankOrMissingFields_returns400AndLeavesUserUnchanged() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(put("/users/{id}", user.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("""
                                {"name":"   ","email":"shopper@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name is required"));

        mockMvc.perform(put("/users/{id}", user.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("""
                                {"name":"Renamed"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is required"));

        var stored = userRepository.findById(user.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Test User");
        assertThat(stored.getEmail()).isEqualTo("shopper@example.com");
    }

    @Test
    void updateUser_withUppercaseEmail_returns400() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(put("/users/{id}", user.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content("""
                                {"name":"Renamed","email":"Shopper@Example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email must be in lowercase"));
    }

    @Test
    void normalUser_canStillDeleteItself() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(delete("/users/{id}", user.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void normalUser_canStillChangeItsPassword() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(post("/users/{id}/change-password", user.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content(CHANGE_PASSWORD_BODY))
                .andExpect(status().isNoContent());

        var stored = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword456", stored.getPassword())).isTrue();
    }

    // Ownership (no IDOR): the target id in the URL is never enough on its
    // own; a normal user can't change, re-password or delete another account.
    @Test
    void normalUser_cannotModifyAnotherNormalUser() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);
        var other = createUser("other@example.com", "password123", Role.USER);

        mockMvc.perform(put("/users/{id}", other.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/users/{id}/change-password", other.getId())
                        .header("Authorization", bearerToken(user))
                        .contentType("application/json")
                        .content(CHANGE_PASSWORD_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/users/{id}", other.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());

        var stored = userRepository.findById(other.getId()).orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("other@example.com");
        assertThat(passwordEncoder.matches("password123", stored.getPassword())).isTrue();
    }

    // ----------------------------------------------------- /auth/me demo flag

    @Test
    void me_flagsTheDemoAccount() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(DEMO_EMAIL))
                .andExpect(jsonPath("$.demoAccount").value(true));
    }

    @Test
    void me_doesNotFlagANormalAccount() throws Exception {
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.demoAccount").value(false));
    }

    // The demo guard runs after the existing ownership check, so a normal
    // user still gets the ordinary 403 for someone else's account.
    @Test
    void normalUser_stillCannotModifyTheDemoAccount() throws Exception {
        var demo = createUser(DEMO_EMAIL, "password123", Role.USER);
        var user = createUser("shopper@example.com", "password123", Role.USER);

        mockMvc.perform(delete("/users/{id}", demo.getId())
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsById(demo.getId())).isTrue();
    }
}
