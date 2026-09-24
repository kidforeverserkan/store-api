package com.kidforeverserkan.store.auth;

import com.kidforeverserkan.store.AbstractIntegrationTest;
import com.kidforeverserkan.store.users.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthAndRegistrationTests extends AbstractIntegrationTest {

    @Test
    void register_withValidData_createsUserAndReturns201() throws Exception {
        var body = """
                {"name":"Jane Doe","email":"jane@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void register_withDuplicateEmail_returns400() throws Exception {
        createUser("jane@example.com", "password123", Role.USER);

        var body = """
                {"name":"Jane Doe","email":"jane@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is already registered."));
    }

    @Test
    void register_withShortPassword_returns400WithValidationError() throws Exception {
        var body = """
                {"name":"Jane Doe","email":"jane@example.com","password":"short"}
                """;

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void login_withValidCredentials_returnsAccessToken() throws Exception {
        createUser("jane@example.com", "password123", Role.USER);

        var body = """
                {"email":"jane@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        createUser("jane@example.com", "password123", Role.USER);

        var body = """
                {"email":"jane@example.com","password":"wrong-password"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        var user = createUser("jane@example.com", "password123", Role.USER);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    // Regression test: a malformed/garbage Bearer token used to propagate an
    // uncaught JwtException out of JwtAuthenticationFilter on every
    // protected endpoint, bypassing GlobalExceptionHandler entirely and
    // producing Spring Boot's generic /error response instead of a clean
    // 401. JwtService#parseToken now treats any unparseable token as "not
    // authenticated" rather than letting the exception propagate.
    @Test
    void protectedEndpoint_withGarbageBearerToken_returns401NotServerError() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // Regression test: POST /auth/refresh without a refreshToken cookie used
    // to throw MissingRequestCookieException, which (after the new
    // catch-all was added) was incorrectly turned into a 500 instead of a
    // clean 401.
    @Test
    void refresh_withoutCookie_returns401NotServerError() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withGarbageCookie_returns401NotServerError() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "garbage.invalid.token")))
                .andExpect(status().isUnauthorized());
    }

    // Regression test: wrong Content-Type used to be swallowed by the
    // catch-all and reported as a 500 instead of 415.
    @Test
    void login_withUnsupportedContentType_returns415NotServerError() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("text/plain")
                        .content("{\"email\":\"jane@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
