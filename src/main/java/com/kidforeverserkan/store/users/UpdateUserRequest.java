package com.kidforeverserkan.store.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// PUT /users/{id} replaces both fields, so both are required: a missing or
// blank value is a 400 here instead of a database constraint error later.
@Data
public class UpdateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be in lowercase")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;
}
