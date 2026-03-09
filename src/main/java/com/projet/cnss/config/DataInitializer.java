package com.projet.cnss.config;

import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        createRoleIfNotExists(ERole.ROLE_ADMIN);
        createRoleIfNotExists(ERole.ROLE_ASSURE);
        createRoleIfNotExists(ERole.ROLE_AGENT_CNSS);
        createRoleIfNotExists(ERole.ROLE_AGENT_BUREAU);
        createRoleIfNotExists(ERole.ROLE_AGENT_DIRECTION);
    }

    private void createRoleIfNotExists(ERole eRole) {
        if (roleRepository.findByName(eRole).isEmpty()) {
            roleRepository.save(new Role(eRole));
            log.info("Rôle créé : {}", eRole);
        } else {
            log.info("Rôle déjà existant : {}", eRole);
        }
    }
}