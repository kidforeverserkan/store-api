package com.kidforeverserkan.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidforeverserkan.store.auth.JwtService;
import com.kidforeverserkan.store.users.Role;
import com.kidforeverserkan.store.users.User;
import com.kidforeverserkan.store.users.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared setup for controller-level integration tests: a real Spring context
 * backed by the in-memory H2 database (see src/test/resources/application.yaml),
 * MockMvc for HTTP-level assertions, and helpers for creating authenticated
 * users without going through the full registration/login flow each time.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User createUser(String email, String rawPassword, Role role) {
        var user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    protected String bearerToken(User user) {
        return "Bearer " + jwtService.generateAccessToken(user).toString();
    }
}
