package com.codewithmosh.store.dtos;
import com.codewithmosh.store.validation.Lowercase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class RegisterUserRequest {
    @NotBlank(message = "name is required")
    @Size(max = 255,message = "name must be less then 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be in lowercase")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8,max = 25,message = "password must be between 8 to 25 characters long")
    private String password;
}
