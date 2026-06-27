package com.projet.cnss.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAgentRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le numéro d'assuré est obligatoire")
    private String numeroAssure;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le CIN est obligatoire")
    private String cin;

    // ✅ @Pattern sans @NotBlank → null accepté, validé seulement si non-null
    @Pattern(
            regexp = "^\\+?[0-9]{8,15}$",
            message = "Format de téléphone invalide"
    )
    private String telephone;

    // ✅ Pas de @Pattern sur password en modification → chaîne vide autorisée
    private String password;

    // ✅ Rôle optionnel
    private String role;
}
