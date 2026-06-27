package com.projet.cnss.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfExtractorService {

    public String extraireTexte(MultipartFile file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // ordre visuel: gauche->droite, haut->bas
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException(
                        "Impossible d'extraire le texte du PDF. " +
                                "Assurez-vous que le PDF n'est pas une image scannee."
                );
            }
            return text.trim();
        }
    }
}