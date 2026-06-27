package com.projet.cnss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @Pattern(
            regexp = "^\\+?[0-9]{8,15}$",
            message = "Format de téléphone invalide"
    )
    private String telephone;
}