package com.projet.cnss.services;

import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.PieceJustificative;
import com.projet.cnss.entity.TypePiece;
import com.projet.cnss.repository.DossierRepository;
import com.projet.cnss.repository.PieceJustificativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Slf4j
public class PieceService {

    private final PieceJustificativeRepository pieceRepository;
    private final DossierRepository dossierRepository;
    private final MinioService minioService;
    private final NotificationService notificationService;

    public PieceService(PieceJustificativeRepository pieceRepository,
                        DossierRepository dossierRepository,
                        MinioService minioService,
                        NotificationService notificationService) {
        this.pieceRepository = pieceRepository;
        this.dossierRepository = dossierRepository;
        this.minioService = minioService;
        this.notificationService = notificationService;
    }

    // Upload une pièce
    public PieceJustificative uploadPiece(Long dossierId, MultipartFile file,
                                          TypePiece typePiece,
                                          String userEmail) throws Exception {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        // Vérifier format fichier
        String ext = getExtension(file.getOriginalFilename()).toUpperCase();
        if (!typePiece.getFormatsAcceptes().contains(ext)) {
            throw new RuntimeException(
                    "Format non accepté pour " + typePiece.getLibelle() +
                            ". Formats acceptés : " + typePiece.getFormatsAcceptes()
            );
        }

        // Upload MinIO dans sous-dossier pieces/
        String objectName = userEmail.replace("@", "_").replace(".", "_")
                + "/dossier_" + dossierId
                + "/pieces/" + typePiece.name().toLowerCase()
                + "_" + System.currentTimeMillis()
                + "_" + file.getOriginalFilename();

        minioService.uploadRaw(objectName, file);

        // Sauvegarder en BDD
        PieceJustificative piece = new PieceJustificative();
        piece.setTypePiece(typePiece);
        piece.setFileName(file.getOriginalFilename());
        piece.setMinioPath(objectName);
        piece.setStatut("EN_ATTENTE");
        piece.setDossier(dossier);
        PieceJustificative saved = pieceRepository.save(piece);

        // Notification aux agents bureau
        notificationService.notifierNouvellepiece(dossier, typePiece);

        log.info("Pièce uploadée : {} pour dossier #{}", typePiece.getLibelle(), dossierId);
        return saved;
    }

    // Lister pièces d'un dossier
    public List<PieceJustificative> getPiecesByDossier(Long dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
        return pieceRepository.findByDossier(dossier);
    }

    // Valider une pièce (agent)
    public PieceJustificative validerPiece(Long pieceId,
                                           boolean valide, String motif) {
        PieceJustificative piece = pieceRepository.findById(pieceId)
                .orElseThrow(() -> new RuntimeException("Pièce non trouvée"));

        piece.setStatut(valide ? "VALIDE" : "REFUSE");
        if (!valide) piece.setMotifRefus(motif);
        PieceJustificative saved = pieceRepository.save(piece);

        // Notification à l'assuré
        notificationService.notifierValidationPiece(
                piece.getDossier(), piece.getTypePiece(), valide, motif
        );
        return saved;
    }

    // Télécharger une pièce
    public byte[] downloadPiece(Long pieceId) throws Exception {
        PieceJustificative piece = pieceRepository.findById(pieceId)
                .orElseThrow(() -> new RuntimeException("Pièce non trouvée"));
        return minioService.downloadDocument(piece.getMinioPath());
    }

    // Liste des pièces requises par type d'avantage
    public Map<String, Object> getPiecesRequises(String typeAvantage) {
        List<Map<String, Object>> piecesList = new ArrayList<>();

        List<TypePiece> piecesCommunes = List.of(
                TypePiece.CIN,
                TypePiece.EXTRAIT_NAISSANCE,
                TypePiece.FORMULAIRE_SIGNE
        );

        List<TypePiece> piecesSpecifiques = switch (typeAvantage != null ? typeAvantage.toUpperCase() : "") {
            case "RETRAITE" -> List.of(
                    TypePiece.ATTESTATION_TRAVAIL,
                    TypePiece.ATTESTATION_SALAIRE
            );
            case "INVALIDITE" -> List.of(
                    TypePiece.CERTIFICAT_MEDICAL,
                    TypePiece.ATTESTATION_TRAVAIL
            );
            case "ACCIDENT_DE_TRAVAIL" -> List.of(
                    TypePiece.CERTIFICAT_MEDICAL,
                    TypePiece.ATTESTATION_TRAVAIL
            );
            case "DECES" -> List.of(
                    TypePiece.CERTIFICAT_MEDICAL
            );
            default -> List.of();
        };

        // Construire la liste complète
        Stream.concat(piecesCommunes.stream(), piecesSpecifiques.stream())
                .forEach(type -> {
                    Map<String, Object> pieceInfo = new LinkedHashMap<>();
                    pieceInfo.put("type", type.name());
                    pieceInfo.put("libelle", type.getLibelle());
                    pieceInfo.put("obligatoire", type.isObligatoire());
                    pieceInfo.put("formatsAcceptes", type.getFormatsAcceptes());
                    piecesList.add(pieceInfo);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typeAvantage", typeAvantage);
        result.put("totalPieces", piecesList.size());
        result.put("piecesObligatoires", piecesList.stream()
                .filter(p -> (boolean) p.get("obligatoire")).count());
        result.put("pieces", piecesList);
        return result;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
