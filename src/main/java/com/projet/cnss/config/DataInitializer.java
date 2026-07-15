package com.projet.cnss.config;

import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        createRoleIfNotExists(ERole.ROLE_ADMIN);
        createRoleIfNotExists(ERole.ROLE_ASSURE);
        createRoleIfNotExists(ERole.ROLE_AGENT_CNSS);
        createRoleIfNotExists(ERole.ROLE_AGENT_BUREAU);
        createRoleIfNotExists(ERole.ROLE_AGENT_DIRECTION);
    }

    @Transactional
    public void createRoleIfNotExists(ERole eRole) {
        if (roleRepository.findByName(eRole).isPresent()) {
            log.info("Rôle déjà existant : {}", eRole);
            return;
        }
        try {
            roleRepository.save(new Role(eRole));
            log.info("Rôle créé : {}", eRole);
        } catch (DataIntegrityViolationException e) {
            log.info("Rôle {} déjà créé par un autre pod, on ignore", eRole);
        }
    }
}