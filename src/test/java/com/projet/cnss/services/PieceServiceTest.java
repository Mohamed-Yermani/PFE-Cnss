package com.projet.cnss.services;

import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.PieceJustificative;
import com.projet.cnss.entity.TypePiece;
import com.projet.cnss.repository.DossierRepository;
import com.projet.cnss.repository.PieceJustificativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PieceServiceTest {

    @Mock private PieceJustificativeRepository pieceRepository;
    @Mock private DossierRepository dossierRepository;
    @Mock private MinioService minioService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PieceService pieceService;

    private Dossier dossier;

    @BeforeEach
    void setUp() {
        dossier = new Dossier();
        dossier.setId(1L);
        dossier.setStatut("EN_ATTENTE");
    }

    // ==========================================================
    // uploadPiece
    // ==========================================================

    @Test
    void uploadPiece_success_formatAccepte() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "cin.pdf",
                "application/pdf", "contenu".getBytes());

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(pieceRepository.save(any(PieceJustificative.class)))
                .thenAnswer(invocation -> {
                    PieceJustificative p = invocation.getArgument(0);
                    p.setId(100L);
                    return p;
                });

        PieceJustificative result = pieceService.uploadPiece(
                1L, file, TypePiece.CIN, "jean@test.com");

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(TypePiece.CIN, result.getTypePiece());
        assertEquals("EN_ATTENTE", result.getStatut());
        assertEquals(dossier, result.getDossier());
        verify(minioService).uploadRaw(anyString(), eq(file));
        verify(notificationService).notifierNouvellepiece(dossier, TypePiece.CIN);
    }

    @Test
    void uploadPiece_dossierNotFound_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "cin.pdf",
                "application/pdf", "contenu".getBytes());

        when(dossierRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pieceService.uploadPiece(99L, file, TypePiece.CIN, "jean@test.com"));

        assertTrue(ex.getMessage().contains("Dossier non trouvé"));
        verify(pieceRepository, never()).save(any());
    }

    @Test
    void uploadPiece_formatNonAccepte_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "cin.docx",
                "application/octet-stream", "contenu".getBytes());

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pieceService.uploadPiece(1L, file, TypePiece.CIN, "jean@test.com"));

        assertTrue(ex.getMessage().contains("Format non accepté"));
        verify(pieceRepository, never()).save(any());
        verifyNoInteractions(minioService);
    }

    @Test
    void uploadPiece_fichierSansExtension_traiteCommeFormatVide_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "cinpdf",
                "application/pdf", "contenu".getBytes());

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));

        assertThrows(RuntimeException.class,
                () -> pieceService.uploadPiece(1L, file, TypePiece.CIN, "jean@test.com"));
    }

    @Test
    void uploadPiece_objectNamePathConstruction_replacesEmailSpecialChars() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "doc.pdf",
                "application/pdf", "contenu".getBytes());

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(pieceRepository.save(any(PieceJustificative.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pieceService.uploadPiece(1L, file, TypePiece.FORMULAIRE_SIGNE, "jean.dupont@test.com");

        verify(minioService).uploadRaw(argThat(name ->
                name.startsWith("jean_dupont_test_com/dossier_1/pieces/formulaire_signe_")
                        && name.endsWith("_doc.pdf")
        ), eq(file));
    }

    // ==========================================================
    // getPiecesByDossier
    // ==========================================================

    @Test
    void getPiecesByDossier_success() {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setDossier(dossier);

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(pieceRepository.findByDossier(dossier)).thenReturn(List.of(piece));

        List<PieceJustificative> result = pieceService.getPiecesByDossier(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getPiecesByDossier_dossierNotFound_throwsException() {
        when(dossierRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pieceService.getPiecesByDossier(99L));

        assertTrue(ex.getMessage().contains("Dossier non trouvé"));
    }

    // ==========================================================
    // validerPiece
    // ==========================================================

    @Test
    void validerPiece_valide_setsStatutValideAndNoMotif() {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setTypePiece(TypePiece.CIN);
        piece.setDossier(dossier);

        when(pieceRepository.findById(1L)).thenReturn(Optional.of(piece));
        when(pieceRepository.save(any(PieceJustificative.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PieceJustificative result = pieceService.validerPiece(1L, true, null);

        assertEquals("VALIDE", result.getStatut());
        assertNull(result.getMotifRefus());
        verify(notificationService).notifierValidationPiece(dossier, TypePiece.CIN, true, null);
    }

    @Test
    void validerPiece_refuse_setsStatutRefuseAndMotif() {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setTypePiece(TypePiece.CIN);
        piece.setDossier(dossier);

        when(pieceRepository.findById(1L)).thenReturn(Optional.of(piece));
        when(pieceRepository.save(any(PieceJustificative.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PieceJustificative result = pieceService.validerPiece(1L, false, "Document illisible");

        assertEquals("REFUSE", result.getStatut());
        assertEquals("Document illisible", result.getMotifRefus());
        verify(notificationService).notifierValidationPiece(dossier, TypePiece.CIN, false, "Document illisible");
    }

    @Test
    void validerPiece_notFound_throwsException() {
        when(pieceRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pieceService.validerPiece(99L, true, null));

        assertTrue(ex.getMessage().contains("Pièce non trouvée"));
    }

    // ==========================================================
    // downloadPiece
    // ==========================================================

    @Test
    void downloadPiece_success() throws Exception {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setMinioPath("path/to/piece.pdf");
        byte[] content = "pdf-content".getBytes();

        when(pieceRepository.findById(1L)).thenReturn(Optional.of(piece));
        when(minioService.downloadDocument("path/to/piece.pdf")).thenReturn(content);

        byte[] result = pieceService.downloadPiece(1L);

        assertArrayEquals(content, result);
    }

    @Test
    void downloadPiece_notFound_throwsException() {
        when(pieceRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pieceService.downloadPiece(99L));

        assertTrue(ex.getMessage().contains("Pièce non trouvée"));
    }

    // ==========================================================
    // getPiecesRequises
    // ==========================================================

    @Test
    void getPiecesRequises_retraite_includesSpecificPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises("RETRAITE");

        assertEquals("RETRAITE", result.get("typeAvantage"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        // 3 communes + 2 spécifiques retraite
        assertEquals(5, pieces.size());
        assertTrue(pieces.stream().anyMatch(p -> "ATTESTATION_TRAVAIL".equals(p.get("type"))));
        assertTrue(pieces.stream().anyMatch(p -> "ATTESTATION_SALAIRE".equals(p.get("type"))));
    }

    @Test
    void getPiecesRequises_invalidite_includesSpecificPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises("invalidite");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        assertEquals(5, pieces.size());
        assertTrue(pieces.stream().anyMatch(p -> "CERTIFICAT_MEDICAL".equals(p.get("type"))));
        assertTrue(pieces.stream().anyMatch(p -> "ATTESTATION_TRAVAIL".equals(p.get("type"))));
    }

    @Test
    void getPiecesRequises_accidentDeTravail_includesSpecificPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises("ACCIDENT_DE_TRAVAIL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        assertEquals(5, pieces.size());
        assertTrue(pieces.stream().anyMatch(p -> "CERTIFICAT_MEDICAL".equals(p.get("type"))));
    }

    @Test
    void getPiecesRequises_deces_includesSpecificPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises("DECES");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        // 3 communes + 1 spécifique deces
        assertEquals(4, pieces.size());
        assertTrue(pieces.stream().anyMatch(p -> "CERTIFICAT_MEDICAL".equals(p.get("type"))));
    }

    @Test
    void getPiecesRequises_typeInconnu_onlyCommonPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises("TYPE_INEXISTANT");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        assertEquals(3, pieces.size());
    }

    @Test
    void getPiecesRequises_typeAvantageNull_onlyCommonPieces() {
        Map<String, Object> result = pieceService.getPiecesRequises(null);

        assertNull(result.get("typeAvantage"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pieces = (List<Map<String, Object>>) result.get("pieces");
        assertEquals(3, pieces.size());
    }

    @Test
    void getPiecesRequises_countsObligatoiresCorrectly() {
        Map<String, Object> result = pieceService.getPiecesRequises("RETRAITE");

        // communes: CIN(true), EXTRAIT_NAISSANCE(true), FORMULAIRE_SIGNE(true)
        // retraite: ATTESTATION_TRAVAIL(true), ATTESTATION_SALAIRE(true)
        assertEquals(5L, result.get("piecesObligatoires"));
        assertEquals(5, result.get("totalPieces"));
    }
}