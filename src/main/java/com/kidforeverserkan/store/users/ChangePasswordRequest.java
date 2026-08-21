package com.kidforeverserkan.store.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(
            min = 8,
            max = 25,
            message = "New password must be between 8 and 25 characters long"
    )
    private String newPassword;
}