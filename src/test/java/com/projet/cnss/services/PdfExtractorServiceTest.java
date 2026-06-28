package com.projet.cnss.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractorServiceTest {

    private final PdfExtractorService pdfExtractorService = new PdfExtractorService();

    /**
     * Génère un vrai PDF (PDFBox) contenant le texte donné, pour tester
     * l'extraction sans avoir à mocker des méthodes statiques.
     */
    private byte[] buildPdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            if (text != null) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    contentStream.beginText();
                    contentStream.setFont(font, 12);
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText(text);
                    contentStream.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void extraireTexte_success_returnsTrimmedExtractedText() throws Exception {
        byte[] pdfBytes = buildPdfWithText("Formulaire de demande CNSS");
        MultipartFile file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", pdfBytes);

        String result = pdfExtractorService.extraireTexte(file);

        assertNotNull(result);
        assertTrue(result.contains("Formulaire de demande CNSS"));
        assertEquals(result.trim(), result); // bien trim()
    }

    @Test
    void extraireTexte_emptyPdf_throwsRuntimeException() throws Exception {
        byte[] pdfBytes = buildPdfWithText(null); // page sans aucun texte
        MultipartFile file = new MockMultipartFile("file", "vide.pdf",
                "application/pdf", pdfBytes);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfExtractorService.extraireTexte(file));

        assertTrue(ex.getMessage().contains("Impossible d'extraire le texte du PDF"));
    }

    @Test
    void extraireTexte_blankWhitespaceOnlyPdf_throwsRuntimeException() throws Exception {
        byte[] pdfBytes = buildPdfWithText("   "); // uniquement des espaces
        MultipartFile file = new MockMultipartFile("file", "espaces.pdf",
                "application/pdf", pdfBytes);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfExtractorService.extraireTexte(file));

        assertTrue(ex.getMessage().contains("Impossible d'extraire le texte du PDF"));
    }

    @Test
    void extraireTexte_invalidPdfBytes_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "invalide.pdf",
                "application/pdf", "ceci n'est pas un PDF valide".getBytes());

        assertThrows(Exception.class, () -> pdfExtractorService.extraireTexte(file));
    }
}