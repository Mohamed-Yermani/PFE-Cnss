package com.projet.cnss.mail;

import com.projet.cnss.entity.EmailDetails;
import com.projet.cnss.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Autowired
    JavaMailSender javaMailSender;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verificationLink = baseUrl + "/api/auth/verify?token=" + user.getVerificationToken();

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Vérification de votre compte");

            // Email au format HTML
            String emailContent = "<html>"
                    + "<body>"
                    + "<h2>Bonjour " + user.getPrenom() + " " + user.getNom() + ",</h2>"
                    + "<p>Merci pour votre inscription. Veuillez cliquer sur le lien ci-dessous pour vérifier votre email :</p>"
                    + "<a href=\"" + verificationLink + "\">Vérifier mon email</a>"
                    + "<p>Ce lien expirera dans 24 heures.</p>"
                    + "<p>Cordialement,<br/>L'équipe de CNSS</p>"
                    + "</body>"
                    + "</html>";

            helper.setText(emailContent, true); // true = HTML

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email de vérification", e);
        }
    }

    public String sendSimpleMail(String to, String subject, String text)
    {

        // Try block to check for exceptions
        try {

            // Creating a simple mail message
            SimpleMailMessage mailMessage
                    = new SimpleMailMessage();
            // Creating a MimeMessage
            MimeMessage message = javaMailSender.createMimeMessage();

            // Creating a helper for the MimeMessage
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);  // Set to true for HTML content

            // Sending the mail
            javaMailSender.send(message);
            return "Mail Sent Successfully...";
        }

        // Catch block to handle the exceptions
        catch (Exception e) {
            return "Error while Sending Mail";
        }
    }

    // Method 2
    // To send an email with attachment
    public String
    sendMailWithAttachment(EmailDetails details)
    {
        // Creating a mime message
        MimeMessage mimeMessage
                = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {

            // Setting multipart as true for attachments to
            // be send
            mimeMessageHelper
                    = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(
                    details.getSubject());

            // Adding the attachment
            FileSystemResource file
                    = new FileSystemResource(
                    new File(details.getAttachment()));

            mimeMessageHelper.addAttachment(
                    file.getFilename(), file);

            // Sending the mail
            javaMailSender.send(mimeMessage);
            return "Mail sent Successfully";
        }

        // Catch block to handle MessagingException
        catch (MessagingException e) {

            // Display message when exception occurred
            return "Error while sending mail!!!";
        }
    }
    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = "http://localhost:4200/reset-password?token=" + resetToken;

        String subject = "Réinitialisation de votre mot de passe";
        String content = "Bonjour " + user.getPrenom() + ",\n\n"
                + "Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le lien ci-dessous :\n"
                + resetLink + "\n\n"
                + "Ce lien expirera dans 1 heure.\n"
                + "Si vous n'avez pas fait cette demande, ignorez cet email.\n\n"
                + "Cordialement,\nL'équipe Location Voiture";

        sendSimpleMail(user.getEmail(), subject, content);
    }

}