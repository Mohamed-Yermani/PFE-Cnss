package com.projet.cnss.services;


import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;


import com.projet.cnss.entity.User;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;


@Service
public class PdfFormService {

    public byte[] genererFormulairePreRempli(User user) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        PdfPage page = pdf.addNewPage(PageSize.A4);


        // ── Couleurs ──────────────────────────────────────────
        Color BLUE      = new DeviceRgb(30, 58, 95);
        Color LIGHTBLUE = new DeviceRgb(46, 117, 182);
        Color ACCENT    = new DeviceRgb(232, 244, 251);
        Color GRAY      = new DeviceRgb(204, 204, 204);
        Color LIGHTGRAY = new DeviceRgb(245, 245, 245);
        Color DARKGRAY  = new DeviceRgb(100, 100, 100);
        Color FILLED    = new DeviceRgb(235, 245, 255); // fond champ pré-rempli
        PdfCanvas canvas = new PdfCanvas(page);

        float W = PageSize.A4.getWidth();
        float H = PageSize.A4.getHeight();


        PdfFont fontBold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ══════════════════════════════════════════════════════
        // EN-TETE
        // ══════════════════════════════════════════════════════
        canvas.setFillColor(BLUE)
                .rectangle(0, H - 35 * mm, W, 35 * mm)
                .fill();

        canvas.setFillColor(ColorConstants.WHITE)
                .setFontAndSize(fontBold, 22)
                .beginText()
                .moveText(18 * mm, H - 20 * mm)
                .showText("CNSS")
                .endText();

        canvas.setFontAndSize(fontNormal, 9)
                .beginText()
                .moveText(18 * mm, H - 27 * mm)
                .showText("Caisse Nationale de Securite Sociale")
                .endText();

        canvas.setFontAndSize(fontBold, 13)
                .beginText()
                .moveText(W / 2 - 85 * mm, H - 18 * mm)
                .showText("FORMULAIRE DE DEPOT DE DOSSIER")
                .endText();

        canvas.setFontAndSize(fontNormal, 8)
                .beginText()
                .moveText(W / 2 - 80 * mm, H - 26 * mm)
                .showText("Informations personnelles pre-remplies - Completer les champs restants")
                .endText();

        // ══════════════════════════════════════════════════════
        // PIED DE PAGE
        // ══════════════════════════════════════════════════════
        canvas.setFillColor(ACCENT)
                .rectangle(0, 0, W, 18 * mm)
                .fill();

        canvas.setStrokeColor(LIGHTBLUE)
                .setLineWidth(1)
                .moveTo(0, 18 * mm)
                .lineTo(W, 18 * mm)
                .stroke();

        canvas.setFillColor(BLUE)
                .setFontAndSize(fontBold, 8)
                .beginText()
                .moveText(15 * mm, 12 * mm)
                .showText("CNSS - Caisse Nationale de Securite Sociale")
                .endText();

        canvas.setFillColor(DARKGRAY)
                .setFontAndSize(fontNormal, 7)
                .beginText()
                .moveText(15 * mm, 7 * mm)
                .showText("Ce formulaire doit etre depose accompagne des pieces justificatives en original")
                .endText();

        // ══════════════════════════════════════════════════════
        // HELPER : dessiner un champ
        // ══════════════════════════════════════════════════════
        // (utilise une lambda-like via méthode privée)

        float y = H - 44 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 1 — IDENTITE DE L'ASSURE (pré-remplie)
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "1. IDENTITE DE L'ASSURE  (pre-rempli depuis votre compte)");

        // Ligne 1 : Nom + Prénom
        drawFieldPreFilled(canvas, fontNormal, fontBold, LIGHTGRAY, FILLED,
                LIGHTBLUE, DARKGRAY,
                15 * mm, y, 85 * mm, "Nom", user.getNom());
        drawFieldPreFilled(canvas, fontNormal, fontBold, LIGHTGRAY, FILLED,
                LIGHTBLUE, DARKGRAY,
                105 * mm, y, 85 * mm, "Prenom", user.getPrenom());
        y -= 14 * mm;

        // Ligne 2 : CIN + Téléphone
        drawFieldPreFilled(canvas, fontNormal, fontBold, LIGHTGRAY, FILLED,
                LIGHTBLUE, DARKGRAY,
                15 * mm, y, 55 * mm, "Numero CIN", user.getCin());
        drawFieldPreFilled(canvas, fontNormal, fontBold, LIGHTGRAY, FILLED,
                LIGHTBLUE, DARKGRAY,
                80 * mm, y, 55 * mm, "Telephone", user.getTelephone());
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                145 * mm, y, 45 * mm, "Date de naissance *");
        y -= 14 * mm;

        // Ligne 3 : Adresse
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 170 * mm, "Adresse complete *");
        y -= 16 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 2 — INFORMATIONS EMPLOYEUR (vide)
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "2. INFORMATIONS EMPLOYEUR");

        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 115 * mm, "Nom de l'employeur / Raison Sociale *");
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                140 * mm, y, 50 * mm, "Code Employeur *");
        y -= 14 * mm;

        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 115 * mm, "Adresse employeur");
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                140 * mm, y, 50 * mm, "Date affiliation *");
        y -= 16 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 3 — TYPE DE DOSSIER
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "3. TYPE DE DOSSIER / AVANTAGE DEMANDE");

        canvas.setFillColor(BLUE)
                .setFontAndSize(fontBold, 8)
                .beginText()
                .moveText(18 * mm, y)
                .showText("Type d'avantage * :")
                .endText();
        y -= 7 * mm;

        String[] types = {
                "Retraite", "Invalidite",
                "Accident de travail", "Maladie professionnelle",
                "Deces", "Autre"
        };

        for (int i = 0; i < types.length; i++) {
            float cx = (i % 2 == 0) ? 22 * mm : 105 * mm;

            int row = i / 2;
            float cy = y - row * 9 * mm;
            // Case à cocher
            canvas.setStrokeColor(LIGHTBLUE)
                    .setFillColor(ColorConstants.WHITE)
                    .setLineWidth(1)
                    .rectangle(cx, cy - mm, 5 * mm, 5 * mm)
                    .fillStroke();
            canvas.setFillColor(ColorConstants.BLACK)
                    .setFontAndSize(fontNormal, 9)
                    .beginText()
                    .moveText(cx + 7 * mm, cy + 2 * mm)
                    .showText(types[i])
                    .endText();
        }
        int rows = types.length / 2;
        y -= rows * 9 * mm + 6 * mm;

        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 80 * mm, "Reference du dossier");
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                105 * mm, y, 50 * mm, "Date de depot *");
        y -= 16 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 4 — PERIODE
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "4. PERIODE DE L'AVANTAGE");

        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 75 * mm, "Date debut avantage *");
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                100 * mm, y, 75 * mm, "Date fin avantage");
        y -= 16 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 5 — OBSERVATION
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "5. OBSERVATION / REMARQUES");

        canvas.setFillColor(LIGHTGRAY)
                .setStrokeColor(GRAY)
                .setLineWidth(0.7f)
                .rectangle(15 * mm, y - 20 * mm, W - 30 * mm, 21 * mm)
                .fillStroke();
        canvas.setFillColor(GRAY)
                .setFontAndSize(fontNormal, 7)
                .beginText()
                .moveText(18 * mm, y - 5 * mm)
                .showText("Mentionnez ici toute information complementaire...")
                .endText();
        y -= 28 * mm;

        // ══════════════════════════════════════════════════════
        // SECTION 6 — SIGNATURE
        // ══════════════════════════════════════════════════════
        y = drawSectionTitle(canvas, fontBold, LIGHTBLUE, ColorConstants.WHITE,
                W, y, "6. DECLARATION ET SIGNATURE");

        canvas.setFillColor(ColorConstants.BLACK)
                .setFontAndSize(fontNormal, 8)
                .beginText()
                .moveText(15 * mm, y)
                .showText("Je soussigne(e) certifie l'exactitude des informations fournies.")
                .endText();
        y -= 10 * mm;

        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                15 * mm, y, 70 * mm, "Fait a (Ville) *");
        drawFieldEmpty(canvas, fontNormal, LIGHTGRAY, GRAY, DARKGRAY,
                95 * mm, y, 45 * mm, "Le (Date) *");

        // Zone signature
        canvas.setFillColor(LIGHTGRAY)
                .setStrokeColor(GRAY).setLineWidth(0.7f)
                .rectangle(150 * mm, y - 6 * mm, 40 * mm, 18 * mm)
                .fillStroke();
        canvas.setFillColor(DARKGRAY)
                .setFontAndSize(fontNormal, 8)
                .beginText()
                .moveText(153 * mm, y + 9 * mm)
                .showText("Signature de l'assure")
                .endText();

        canvas.setFontAndSize(fontNormal, 7)
                .beginText()
                .moveText(159 * mm, y + 4 * mm)
                .showText("(obligatoire)")
                .endText();

        pdf.close();
        return baos.toByteArray();
    }

    // ── drawSectionTitle ──────────────────────────────────────
    private float drawSectionTitle(PdfCanvas canvas, PdfFont fontBold,
                                   Color bg, Color fg,
                                   float W, float y, String title) {
        canvas.setFillColor(bg)
                .rectangle(15 * mm, y - 6 * mm, W - 30 * mm, 8 * mm)
                .fill();
        canvas.setFillColor(fg)
                .setFontAndSize(fontBold, 9)
                .beginText()
                .moveText(18 * mm, y - 2.5f * mm)
                .showText(title)
                .endText();
        return y - 14 * mm;
    }

    // ── Champ pré-rempli (données BDD) ───────────────────────
    private void drawFieldPreFilled(PdfCanvas canvas,
                                    PdfFont fontNormal, PdfFont fontBold,
                                    Color bgEmpty, Color bgFilled,
                                    Color borderColor, Color labelColor,
                                    float x, float y, float width,
                                    String label, String value) {
        // Label
        canvas.setFillColor(labelColor)
                .setFontAndSize(fontNormal, 8)
                .beginText()
                .moveText(x, y + 2 * mm)
                .showText(label + " :")
                .endText();

        // Fond bleu clair = données pré-remplies
        canvas.setFillColor(bgFilled)
                .setStrokeColor(borderColor)
                .setLineWidth(1f)
                .rectangle(x, y - 6 * mm, width, 7.5f * mm)
                .fillStroke();

        // Valeur
        String displayValue = (value != null && !value.isBlank()) ? value : "";
        canvas.setFillColor(new DeviceRgb(30, 58, 95))
                .setFontAndSize(fontBold, 9)
                .beginText()
                .moveText(x + 2 * mm, y - 2.5f * mm)
                .showText(displayValue)
                .endText();

        // Icône cadenas (champ non modifiable visuellement)
        canvas.setFillColor(new DeviceRgb(46, 117, 182))
                .setFontAndSize(fontNormal, 7)
                .beginText()
                .moveText(x + width - 8 * mm, y - 2.5f * mm)
                .showText("auto")
                .endText();
    }

    // ── Champ vide (à remplir par l'assuré) ──────────────────
    private void drawFieldEmpty(PdfCanvas canvas, PdfFont fontNormal,
                                Color bg, Color border, Color labelColor,
                                float x, float y, float width, String label) {
        canvas.setFillColor(labelColor)
                .setFontAndSize(fontNormal, 8)
                .beginText()
                .moveText(x, y + 2 * mm)
                .showText(label)
                .endText();

        canvas.setFillColor(bg)
                .setStrokeColor(border)
                .setLineWidth(0.7f)
                .rectangle(x, y - 6 * mm, width, 7.5f * mm)
                .fillStroke();
    }

    private float mm = 2.834f; // 1mm en points PDF
}