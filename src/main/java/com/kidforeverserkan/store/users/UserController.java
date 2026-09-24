package com.kidforeverserkan.store.users;

import com.kidforeverserkan.store.auth.AuthService;
import com.kidforeverserkan.store.exceptions.ErrorDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Set;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final DemoAccountProperties demoAccount;

    @GetMapping
    public Iterable<UserDto> getAllUsers(
            @RequestParam(
                    required = false,
                    defaultValue = "",
                    name = "sort"
            ) String sort
    ) {
        requireAdmin();

        if (!Set.of("name", "email").contains(sort)) {
            sort = "name";
        }

        return userRepository.findAll(Sort.by(sort))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {

        checkUserAccess(id);

        var user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody RegisterUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                    Map.of("email", "Email is already registered.")
            );
        }

        var user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);

        var userDto = userMapper.toDto(user);

        var uri = uriBuilder
                .path("/users/{id}")
                .buildAndExpand(userDto.getId())
                .toUri();

        return ResponseEntity
                .created(uri)
                .body(userDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        checkUserAccess(id);

        var user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        rejectDemoAccountChanges(user);

        userMapper.update(request, user);
        userRepository.save(user);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        checkUserAccess(id);

        var user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        rejectDemoAccountChanges(user);

        userRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        checkUserAccess(id);

        var user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        rejectDemoAccountChanges(user);

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPassword()
        )) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private void requireAdmin() {
        if (!isAdmin(authService.getCurrentUser())) {
            throw new AccessDeniedException(
                    "Only administrators can access all users."
            );
        }
    }

    private void checkUserAccess(Long userId) {
        var currentUser = authService.getCurrentUser();

        if (!isAdmin(currentUser)
                && !currentUser.getId().equals(userId)) {

            throw new AccessDeniedException(
                    "You don't have access to this user."
            );
        }
    }

    // The public demo account's login is shared with every visitor, so only
    // an admin may change or delete it; everyone else gets a 403.
    private void rejectDemoAccountChanges(User user) {
        if (demoAccount.isDemoAccount(user)
                && !isAdmin(authService.getCurrentUser())) {
            throw new DemoAccountModificationException();
        }
    }

    @ExceptionHandler(DemoAccountModificationException.class)
    public ResponseEntity<ErrorDto> handleDemoAccountModification(
            DemoAccountModificationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto(ex.getMessage()));
    }
}