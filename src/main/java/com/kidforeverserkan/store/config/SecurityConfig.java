package com.kidforeverserkan.store.config;

import com.kidforeverserkan.store.filters.JwtAuthenticationFilter;
import com.kidforeverserkan.store.users.Role;
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

                        // ----------------------------
                        // ADMIN ENDPOINTS
                        // ----------------------------

                        .requestMatchers("/admin/**")
                        .hasRole(Role.ADMIN.name())

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
}