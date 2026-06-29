package com.projet.cnss.config;

import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_allRolesDoNotExist_createsAllRoles() {
        when(roleRepository.findByName(any(ERole.class))).thenReturn(Optional.empty());

        dataInitializer.run();

        verify(roleRepository, times(5)).save(any(Role.class));
    }

    @Test
    void run_allRolesAlreadyExist_doesNotCreateAnyRole() {
        Role existingRole = new Role(ERole.ROLE_ADMIN);
        when(roleRepository.findByName(any(ERole.class))).thenReturn(Optional.of(existingRole));

        dataInitializer.run();

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void run_someRolesExistSomeDoNot_createsOnlyMissingRoles() {
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(new Role(ERole.ROLE_ADMIN)));
        when(roleRepository.findByName(ERole.ROLE_ASSURE)).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_AGENT_CNSS)).thenReturn(Optional.of(new Role(ERole.ROLE_AGENT_CNSS)));
        when(roleRepository.findByName(ERole.ROLE_AGENT_BUREAU)).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_AGENT_DIRECTION)).thenReturn(Optional.of(new Role(ERole.ROLE_AGENT_DIRECTION)));

        dataInitializer.run();

        verify(roleRepository, times(2)).save(any(Role.class));
    }
}