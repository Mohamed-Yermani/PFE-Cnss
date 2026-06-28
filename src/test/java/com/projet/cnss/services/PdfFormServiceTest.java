package com.projet.cnss.services;

import com.projet.cnss.entity.User;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfFormServiceTest {

    private final PdfFormService pdfFormService = new PdfFormService();

    private User buildUser(String nom, String prenom, String cin, String telephone) {
        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setCin(cin);
        user.setTelephone(telephone);
        return user;
    }

    private String extractText(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    @Test
    void genererFormulairePreRempli_withCompleteUser_generatesValidPdfWithUserData() throws Exception {
        User user = buildUser("Dupont", "Jean", "CIN12345", "20123456");

        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Vérifie que le PDF généré est valide et contient les données pré-remplies
        String text = extractText(pdfBytes);
        assertTrue(text.contains("Dupont"));
        assertTrue(text.contains("Jean"));
        assertTrue(text.contains("CIN12345"));
        assertTrue(text.contains("20123456"));
        assertTrue(text.contains("CNSS"));
        assertTrue(text.contains("FORMULAIRE DE DEPOT DE DOSSIER"));
    }

    @Test
    void genererFormulairePreRempli_withNullOrBlankFields_usesEmptyValueBranch() throws Exception {
        // cin et telephone null/blank pour couvrir la branche "displayValue = """
        User user = buildUser("Martin", "Alice", null, "   ");

        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String text = extractText(pdfBytes);
        assertTrue(text.contains("Martin"));
        assertTrue(text.contains("Alice"));
        // Le document doit rester valide même avec des champs vides
        assertTrue(text.contains("IDENTITE DE L'ASSURE"));
    }

    @Test
    void genererFormulairePreRempli_containsAllSectionTitles() throws Exception {
        User user = buildUser("Nom", "Prenom", "CIN1", "12345678");

        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);
        String text = extractText(pdfBytes);

        assertTrue(text.contains("INFORMATIONS EMPLOYEUR"));
        assertTrue(text.contains("TYPE DE DOSSIER"));
        assertTrue(text.contains("PERIODE DE L'AVANTAGE"));
        assertTrue(text.contains("OBSERVATION"));
        assertTrue(text.contains("DECLARATION ET SIGNATURE"));
    }

    @Test
    void genererFormulairePreRempli_containsAllAvantageTypeCheckboxes() throws Exception {
        User user = buildUser("Nom", "Prenom", "CIN1", "12345678");

        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);
        String text = extractText(pdfBytes);

        assertTrue(text.contains("Retraite"));
        assertTrue(text.contains("Invalidite"));
        assertTrue(text.contains("Accident de travail"));
        assertTrue(text.contains("Maladie professionnelle"));
        assertTrue(text.contains("Deces"));
        assertTrue(text.contains("Autre"));
    }
}