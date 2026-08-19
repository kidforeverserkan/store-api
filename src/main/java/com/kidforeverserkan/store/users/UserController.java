package com.kidforeverserkan.store.users;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public Iterable <UserDto> getAllUsers(
            @RequestHeader(name = "x-auth-token") String authToken,
            @RequestParam(required = false,defaultValue = "",name = "sort") String sort
    ) {
        System.out.println(authToken);

        if (!Set.of("name", "email").contains(sort)) {
            sort = "name";
        }

       return userRepository.findAll(Sort.by(sort))
               .stream()
               .map(userMapper::toDto)
               .toList();
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser (@PathVariable long id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping
    public ResponseEntity <?> createUser ( @Valid @RequestBody RegisterUserRequest request,
                                                UriComponentsBuilder uriBuilder)
    {
        if (userRepository.existsByEmail((request.getEmail()))){
            return ResponseEntity.badRequest().body(
                    Map.of("email","Email is already registered.")
            );
    }
        var user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
        var userDto = userMapper.toDto(user);
       var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getId()).toUri();

        return  ResponseEntity.created(uri).body(userDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity <UserDto> UpdateUser (@PathVariable(name = "id") Long id,
                               @RequestBody UpdateUserRequest request) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        userMapper.update(request,user);
        userRepository.save(user);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deleteUser (@PathVariable long id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/change-password")
    public ResponseEntity <Void> changePassword(
            @PathVariable long id,
            @RequestBody ChangePasswordRequest request){
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }
}


