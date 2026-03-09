package com.projet.cnss.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String numeroAssure;
    private String nom;
    private String prenom;
    private String cin;
    private String telephone;
    private boolean enabled;
    private Set<String> roles;
}
