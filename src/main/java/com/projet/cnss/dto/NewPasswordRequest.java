package com.projet.cnss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewPasswordRequest {
    @NotBlank(message = "Token est obligatoire")
    private String token;

    @NotBlank(message = "Nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String newPassword;
}
