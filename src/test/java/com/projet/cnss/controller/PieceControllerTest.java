package com.projet.cnss.controller;

import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.PieceJustificative;
import com.projet.cnss.entity.TypePiece;
import com.projet.cnss.services.PieceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PieceControllerTest {

    @Mock
    private PieceService pieceService;

    @InjectMocks
    private PieceController pieceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pieceController).build();
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    // ==========================================================
    // POST /api/pieces/upload
    // ==========================================================

    @Test
    void uploadPiece_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");

        MockMultipartFile file = new MockMultipartFile("file", "cin.pdf",
                "application/pdf", "contenu".getBytes());

        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setTypePiece(TypePiece.CIN);
        piece.setStatut("EN_ATTENTE");

        when(pieceService.uploadPiece(eq(1L), any(), eq(TypePiece.CIN), eq("jean@test.com")))
                .thenReturn(piece);

        mockMvc.perform(multipart("/api/pieces/upload")
                        .file(file)
                        .param("dossierId", "1")
                        .param("typePiece", "CIN")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    // ==========================================================
    // GET /api/pieces/dossier/{dossierId}
    // ==========================================================

    @Test
    void getPiecesByDossier_success() throws Exception {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setTypePiece(TypePiece.CIN);

        when(pieceService.getPiecesByDossier(1L)).thenReturn(List.of(piece));

        mockMvc.perform(get("/api/pieces/dossier/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/pieces/requises
    // ==========================================================

    @Test
    void getPiecesRequises_success() throws Exception {
        Map<String, Object> result = Map.of(
                "typeAvantage", "RETRAITE",
                "totalPieces", 5,
                "pieces", List.of()
        );

        when(pieceService.getPiecesRequises("RETRAITE")).thenReturn(result);

        mockMvc.perform(get("/api/pieces/requises").param("type", "RETRAITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeAvantage").value("RETRAITE"));
    }

    // ==========================================================
    // GET /api/pieces/{id}/download
    // ==========================================================

    @Test
    void downloadPiece_success() throws Exception {
        byte[] content = "pdf-bytes".getBytes();
        when(pieceService.downloadPiece(1L)).thenReturn(content);

        mockMvc.perform(get("/api/pieces/1/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    // ==========================================================
    // PUT /api/pieces/{id}/valider
    // ==========================================================

    @Test
    void validerPiece_valideTrue_success() throws Exception {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setStatut("VALIDE");

        when(pieceService.validerPiece(1L, true, null)).thenReturn(piece);

        mockMvc.perform(put("/api/pieces/1/valider").param("valide", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));
    }

    @Test
    void validerPiece_valideFalseAvecMotif_success() throws Exception {
        PieceJustificative piece = new PieceJustificative();
        piece.setId(1L);
        piece.setStatut("REFUSE");
        piece.setMotifRefus("Document illisible");

        when(pieceService.validerPiece(1L, false, "Document illisible")).thenReturn(piece);

        mockMvc.perform(put("/api/pieces/1/valider")
                        .param("valide", "false")
                        .param("motif", "Document illisible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REFUSE"));
    }
}