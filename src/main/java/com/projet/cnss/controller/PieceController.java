package com.projet.cnss.controller;

import com.projet.cnss.entity.PieceJustificative;
import com.projet.cnss.entity.TypePiece;
import com.projet.cnss.services.PieceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pieces")

public class PieceController {

    private final PieceService pieceService;

    public PieceController(PieceService pieceService) {
        this.pieceService = pieceService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<PieceJustificative> uploadPiece(
            @RequestParam("dossierId") Long dossierId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typePiece") TypePiece typePiece,
            Authentication authentication) throws Exception {
        return ResponseEntity.ok(pieceService.uploadPiece(dossierId, file, typePiece, authentication.getName()));
    }

    @GetMapping("/dossier/{dossierId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PieceJustificative>> getPiecesByDossier(@PathVariable Long dossierId) {
        return ResponseEntity.ok(pieceService.getPiecesByDossier(dossierId));
    }

    @GetMapping("/requises")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getPiecesRequises(@RequestParam("type") String typeAvantage) {
        return ResponseEntity.ok(pieceService.getPiecesRequises(typeAvantage));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadPiece(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(pieceService.downloadPiece(id));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_BUREAU', 'ROLE_AGENT_DIRECTION', 'ROLE_AGENT_CNSS', 'ROLE_ADMIN')")
    public ResponseEntity<PieceJustificative> validerPiece(
            @PathVariable Long id,
            @RequestParam("valide") boolean valide,
            @RequestParam(value = "motif", required = false) String motif) {
        return ResponseEntity.ok(pieceService.validerPiece(id, valide, motif));
    }
}
