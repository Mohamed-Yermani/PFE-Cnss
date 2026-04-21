package com.projet.cnss.services;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PdfAnalysisService {

    public Map<String, String> extractFields(byte[] pdfBytes) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new java.io.ByteArrayInputStream(pdfBytes)));

        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, false);

        Map<String, String> data = new HashMap<>();

        if (form != null) {
            for (Map.Entry<String, PdfFormField> entry : form.getFormFields().entrySet()) {
                data.put(entry.getKey(), entry.getValue().getValueAsString());
            }
        }

        pdfDoc.close();
        return data;
    }
}