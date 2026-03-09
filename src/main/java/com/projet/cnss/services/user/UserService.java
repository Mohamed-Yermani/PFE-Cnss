package com.projet.cnss.services.user;

import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    // Récupération basée sur l'email (par ex. obtenu via Authentication.getName())
    public UserDto getCurrentUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .numeroAssure(user.getNumeroAssure())
                .cin(user.getCin())
                .telephone(user.getTelephone())
                .enabled(user.isEnabled())

                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet()))
                .build();
    }
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur avec id " + id + " introuvable"));

        return mapToDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // 🔹 Méthode utilitaire pour mapper User → UserDto
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .enabled(user.isEnabled())

                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet()))
                .build();
    }


}