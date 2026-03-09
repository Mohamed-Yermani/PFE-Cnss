package com.projet.cnss.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequest {
    @Email(message = "Email invalide")
    @NotBlank(message = "Email est obligatoire")
    private String email;

    @NotBlank(message = "Mot de passe est obligatoire")
    private String password;
}
