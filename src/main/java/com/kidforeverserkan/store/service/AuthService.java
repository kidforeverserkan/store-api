package com.kidforeverserkan.store.service;

import com.kidforeverserkan.store.entities.User;
import com.kidforeverserkan.store.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@AllArgsConstructor
@Service
public class AuthService {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long)  authentication.getPrincipal();

        return  userRepository.findById(userId).orElse(null);
    }
}
