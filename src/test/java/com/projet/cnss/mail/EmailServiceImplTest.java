package com.projet.cnss.mail;

import com.projet.cnss.entity.EmailDetails;
import com.projet.cnss.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender; // utilisé par sendVerificationEmail / sendPasswordResetEmail

    @Mock
    private JavaMailSender javaMailSender; // utilisé par sendSimpleMail / sendMailWithAttachment

    private EmailServiceImpl emailService;

    private static final Session SESSION = Session.getDefaultInstance(new Properties());

    @BeforeEach
    void setUp() {
        // Constructeur généré par @RequiredArgsConstructor : seul "mailSender" est final
        emailService = new EmailServiceImpl(mailSender);

        // "javaMailSender" est un champ @Autowired séparé, à injecter manuellement
        ReflectionTestUtils.setField(emailService, "javaMailSender", javaMailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@cnss.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8089");
    }

    private User buildUser(String email, String nom, String prenom, String verificationToken) {
        User user = new User();
        user.setEmail(email);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setVerificationToken(verificationToken);
        return user;
    }

    // ==========================================================
    // sendVerificationEmail
    // ==========================================================

    @Test
    void sendVerificationEmail_success_sendsMimeMessage() {
        User user = buildUser("jean@test.com", "Dupont", "Jean", "token123");
        MimeMessage realMessage = new MimeMessage(SESSION);

        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        emailService.sendVerificationEmail(user);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(realMessage);
    }

    @Test
    void sendVerificationEmail_exceptionDuringSend_throwsRuntimeException() {
        User user = buildUser("jean@test.com", "Dupont", "Jean", "token123");
        MimeMessage realMessage = new MimeMessage(SESSION);

        when(mailSender.createMimeMessage()).thenReturn(realMessage);
        doThrow(new RuntimeException("Erreur SMTP")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendVerificationEmail(user));

        assertTrue(ex.getMessage().contains("Échec de l'envoi de l'email de vérification"));
        assertNotNull(ex.getCause());
    }

    @Test
    void sendVerificationEmail_createMimeMessageThrows_isWrappedInRuntimeException() {
        User user = buildUser("jean@test.com", "Dupont", "Jean", "token123");

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connexion SMTP impossible"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendVerificationEmail(user));

        assertTrue(ex.getMessage().contains("Échec de l'envoi de l'email de vérification"));
    }

    // ==========================================================
    // sendSimpleMail
    // ==========================================================

    @Test
    void sendSimpleMail_success_returnsSuccessMessage() {
        MimeMessage realMessage = new MimeMessage(SESSION);
        when(javaMailSender.createMimeMessage()).thenReturn(realMessage);

        String result = emailService.sendSimpleMail("jean@test.com", "Sujet test", "Contenu du message");

        assertEquals("Mail Sent Successfully...", result);
        verify(javaMailSender).send(realMessage);
    }

    @Test
    void sendSimpleMail_exceptionDuringSend_returnsErrorMessage() {
        MimeMessage realMessage = new MimeMessage(SESSION);
        when(javaMailSender.createMimeMessage()).thenReturn(realMessage);
        doThrow(new RuntimeException("Erreur SMTP")).when(javaMailSender).send(any(MimeMessage.class));

        String result = emailService.sendSimpleMail("jean@test.com", "Sujet test", "Contenu du message");

        assertEquals("Error while Sending Mail", result);
    }

    @Test
    void sendSimpleMail_createMimeMessageThrows_returnsErrorMessage() {
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("Connexion impossible"));

        String result = emailService.sendSimpleMail("jean@test.com", "Sujet test", "Contenu du message");

        assertEquals("Error while Sending Mail", result);
    }

    // ==========================================================
    // sendMailWithAttachment
    // ==========================================================

    @Test
    void sendMailWithAttachment_success_returnsSuccessMessage() throws IOException {
        MimeMessage realMessage = new MimeMessage(SESSION);
        when(javaMailSender.createMimeMessage()).thenReturn(realMessage);

        // Crée un vrai fichier temporaire pour que FileSystemResource/addAttachment réussisse
        File tempFile = Files.createTempFile("piece-jointe", ".txt").toFile();
        Files.writeString(tempFile.toPath(), "contenu de test");
        tempFile.deleteOnExit();

        EmailDetails details = new EmailDetails();
        details.setRecipient("jean@test.com");
        details.setMsgBody("Voici votre document.");
        details.setSubject("Document joint");
        details.setAttachment(tempFile.getAbsolutePath());

        String result = emailService.sendMailWithAttachment(details);

        assertEquals("Mail sent Successfully", result);
        verify(javaMailSender).send(realMessage);
    }

    @Test
    void sendMailWithAttachment_sendFails_returnsErrorMessage() throws IOException {
        MimeMessage realMessage = new MimeMessage(SESSION);
        when(javaMailSender.createMimeMessage()).thenReturn(realMessage);

        File file = File.createTempFile("test", ".txt");
        file.deleteOnExit();

        EmailDetails details = new EmailDetails();
        details.setRecipient("jean@test.com");
        details.setSubject("Sujet");
        details.setMsgBody("Message");
        details.setAttachment(file.getAbsolutePath());

        doThrow(new RuntimeException("SMTP Error"))
                .when(javaMailSender)
                .send(any(MimeMessage.class));

        String result = emailService.sendMailWithAttachment(details);

        assertEquals("Error while sending mail!!!", result);
    }

    // ==========================================================
    // sendPasswordResetEmail
    // ==========================================================


}