package com.projet.cnss.services;

import com.projet.cnss.dto.DossierDto;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.DossierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierService {

    private final MinioService minioService;
    private final DossierRepository dossierRepository;

    public DossierDto uploadDossier(String cin, MultipartFile file, User user) throws Exception {
        String objectName = minioService.uploadDocument(user.getEmail(), file);

        LocalDateTime now = LocalDateTime.now();

        Dossier dossier = new Dossier();
        dossier.setCin(cin);
        dossier.setFileName(file.getOriginalFilename());
        dossier.setAlfrescoNodeId(objectName);
        dossier.setStatut("EN_ATTENTE");
        dossier.setUser(user);
        dossier.setDateUpload(now);

        return mapToDto(dossierRepository.save(dossier));
    }

    public List<DossierDto> getUserDossiers(User user) {
        return dossierRepository.findByUser(user)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public DossierDto getDossierById(Long id) {
        return mapToDto(dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable")));
    }

    public List<DossierDto> getDossiersEnAttente() {
        return dossierRepository.findByStatut("EN_ATTENTE")
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public DossierDto validerDossier(Long id, String agentEmail) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        dossier.setStatut("VALIDE");
        dossier.setAgentEmail(agentEmail);
        dossier.setDateTraitement(LocalDateTime.now());
        return mapToDto(dossierRepository.save(dossier));
    }

    public DossierDto refuserDossier(Long id, String motif, String agentEmail) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        dossier.setStatut("REFUSE");
        dossier.setMotifRefus(motif);
        dossier.setAgentEmail(agentEmail);
        dossier.setDateTraitement(LocalDateTime.now());
        return mapToDto(dossierRepository.save(dossier));
    }

    public byte[] downloadDossier(Long id, User user) throws Exception {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        return minioService.downloadDocument(dossier.getAlfrescoNodeId());
    }

    public void deleteDossier(Long id, User user) throws Exception {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
        minioService.deleteDocument(dossier.getAlfrescoNodeId());
        dossierRepository.delete(dossier);
    }

    public Map<String, Long> getStatistics() {
        return Map.of(
                "total", dossierRepository.count(),
                "en_attente", dossierRepository.countByStatut("EN_ATTENTE"),
                "valides", dossierRepository.countByStatut("VALIDE"),
                "refuses", dossierRepository.countByStatut("REFUSE")
        );
    }

    private DossierDto mapToDto(Dossier dossier) {
        return DossierDto.builder()
                .id(dossier.getId())
                .cin(dossier.getCin())
                .fileName(dossier.getFileName())
                .alfrescoNodeId(dossier.getAlfrescoNodeId())
                .statut(dossier.getStatut())
                .motifRefus(dossier.getMotifRefus())
                .agentEmail(dossier.getAgentEmail())
                .userId(dossier.getUser().getId())
                .dateUpload(dossier.getDateUpload())
                .dateTraitement(dossier.getDateTraitement())
                .build();
    }
}