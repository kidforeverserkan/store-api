package com.kidforeverserkan.store.config;

import com.kidforeverserkan.store.filters.JwtAuthenticationFilter;
import com.kidforeverserkan.store.users.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AllArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // We are using JWT, so we don't need HTTP sessions.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Disable CSRF because this is a stateless REST API.
                .csrf(AbstractHttpConfigurer::disable)

                // Configure which endpoints are public and protected.
                .authorizeHttpRequests(auth -> auth

                        // ----------------------------
                        // PUBLIC ENDPOINTS
                        // ----------------------------

                        // Landing page, storefront page shells, and static
                        // assets. These are Thymeleaf page loads, not API
                        // calls — the browser has no way to attach a Bearer
                        // token to a plain navigation, so every page must be
                        // publicly loadable. Pages that need a logged-in user
                        // (checkout, orders) enforce that client-side by
                        // calling the already-protected API, which still
                        // returns 401 for anyone without a valid token.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/shop",
                                "/shop/**",
                                "/cart",
                                "/login",
                                "/register",
                                "/checkout-success",
                                "/checkout-cancel",
                                "/about",
                                "/contact",
                                // Spring Boot's error page. Without this, any
                                // error forwarded here (e.g. GET /shop/abc)
                                // was masked as a blank 401 for anonymous
                                // visitors instead of its real status.
                                "/error",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // "/orders" is deliberately excluded from the blanket
                        // page rule above: it's the one page path that's
                        // identical to an existing, auth-required API path
                        // (OrderController's GET /orders). A plain path
                        // matcher can't tell the two apart — only the
                        // request's Accept header (via `produces` on each
                        // @GetMapping) decides which handler runs, so the
                        // security rule must check the same thing, or a
                        // request with Accept: application/json would
                        // bypass authentication for the real order data
                        // while still being routed to the API method.
                        .requestMatchers(
                                request -> "GET".equals(request.getMethod())
                                        && "/orders".equals(request.getRequestURI())
                                        && prefersHtml(request)
                        ).permitAll()

                        // Guest carts have no user ownership (identified only
                        // by an unguessable UUID) and browsing the catalog
                        // shouldn't require an account, so these stay public.
                        // This is a deliberate trade-off for a small
                        // portfolio storefront, not a security upgrade.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/products",
                                "/products/**"
                        ).permitAll()

                        .requestMatchers("/carts/**").permitAll()

                        // Exchange rates for the storefront's currency
                        // selector (read-only, no user data).
                        .requestMatchers(HttpMethod.GET, "/currencies").permitAll()

                        // Create a new user.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/users"
                        ).permitAll()

                        // Login and receive JWT tokens.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/login"
                        ).permitAll()

                        // Refresh access token.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/refresh"
                        ).permitAll()

                        // Stripe webhook.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/checkout/webhook"
                        ).permitAll()

                        // Swagger UI.
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Health check (used by Railway).
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health"
                        ).permitAll()

                        // ----------------------------
                        // ADMIN ENDPOINTS
                        // ----------------------------

                        .requestMatchers("/admin/**")
                        .hasRole(Role.ADMIN.name())

                        // Product catalog management is admin-only; browsing
                        // (GET) is public — see the permitAll rule above.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/products/**"
                        ).hasRole(Role.ADMIN.name())

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/**"
                        ).hasRole(Role.ADMIN.name())

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/products/**"
                        ).hasRole(Role.ADMIN.name())

                        // ----------------------------
                        // EVERYTHING ELSE
                        // ----------------------------

                        .anyRequest().authenticated()
                )

                // Check JWT before Spring Security's username/password filter.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Return 401 when authentication is required but missing.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(
                                                HttpStatus.UNAUTHORIZED
                                        )
                                )
                                .accessDeniedHandler(
                                        (request, response, accessDeniedException) ->
                                                response.setStatus(
                                                        HttpStatus.FORBIDDEN.value()
                                                )
                                )
                );

        return http.build();
    }

    // Matches what real browsers send on a page navigation (they always put
    // text/html first) without matching a bare `fetch()` default of `*/*` or
    // an explicit `application/json` — both of those fall through to
    // `.anyRequest().authenticated()` and correctly require a token.
    private boolean prefersHtml(HttpServletRequest request) {
        var accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}