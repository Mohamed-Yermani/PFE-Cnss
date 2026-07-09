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
import java.time.Year;

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

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final String BRAND_NAME = "CNSS";
    private static final String BRAND_COLOR = "#1a56db";

    @Override
    public void sendVerificationEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verificationLink = baseUrl + "/api/auth/verify?token=" + user.getVerificationToken();

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Vérifiez votre adresse email - " + BRAND_NAME);
            helper.setText(buildVerificationTemplate(user, verificationLink), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email de vérification", e);
        }
    }

    public String sendSimpleMail(String to, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            javaMailSender.send(message);
            return "Mail Sent Successfully...";
        } catch (Exception e) {
            return "Error while Sending Mail";
        }
    }

    // Method 2
    // To send an email with attachment
    public String sendMailWithAttachment(EmailDetails details) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper =
                    new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(details.getSubject());

            FileSystemResource file =
                    new FileSystemResource(new File(details.getAttachment()));

            mimeMessageHelper.addAttachment(file.getFilename(), file);

            javaMailSender.send(mimeMessage);

            return "Mail sent Successfully";

        } catch (Exception e) {
            return "Error while sending mail!!!";
        }
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Réinitialisation de votre mot de passe - " + BRAND_NAME;
        String htmlContent = buildPasswordResetTemplate(user, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email de réinitialisation", e);
        }
    }

    // ==================== TEMPLATES ====================

    private String buildVerificationTemplate(User user, String verificationLink) {
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f4f5f7; font-family: 'Segoe UI', Arial, sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding: 40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background-color:%s; padding: 32px 40px; text-align:center;">
                          <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:600; letter-spacing:0.5px;">%s</h1>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding: 40px;">
                          <h2 style="color:#1f2937; font-size:20px; margin-top:0;">Bonjour %s %s,</h2>
                          <p style="color:#4b5563; font-size:15px; line-height:1.6;">
                            Merci de vous être inscrit(e). Pour activer votre compte et commencer à utiliser nos services, veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous.
                          </p>

                          <table role="presentation" cellpadding="0" cellspacing="0" style="margin: 32px 0;">
                            <tr>
                              <td align="center" style="border-radius:6px; background-color:%s;">
                                <a href="%s" target="_blank" style="display:inline-block; padding: 14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:6px;">
                                  Vérifier mon adresse email
                                </a>
                              </td>
                            </tr>
                          </table>

                          <p style="color:#6b7280; font-size:13px; line-height:1.6;">
                            Si le bouton ne fonctionne pas, copiez-collez ce lien dans votre navigateur :<br>
                            <a href="%s" style="color:%s; word-break:break-all;">%s</a>
                          </p>

                          <p style="color:#9ca3af; font-size:13px; line-height:1.6; margin-top:24px;">
                            Ce lien expirera dans 24 heures. Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background-color:#f9fafb; padding: 24px 40px; text-align:center; border-top:1px solid #e5e7eb;">
                          <p style="color:#9ca3af; font-size:12px; margin:0;">
                            © %s %s. Tous droits réservés.
                          </p>
                          <p style="color:#9ca3af; font-size:12px; margin:6px 0 0;">
                            Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                BRAND_COLOR, BRAND_NAME,
                prenom, nom,
                BRAND_COLOR, verificationLink,
                verificationLink, BRAND_COLOR, verificationLink,
                Year.now(), BRAND_NAME
        );
    }

    private String buildPasswordResetTemplate(User user, String resetLink) {
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f4f5f7; font-family: 'Segoe UI', Arial, sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding: 40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08);">

                      <tr>
                        <td style="background-color:%s; padding: 32px 40px; text-align:center;">
                          <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:600; letter-spacing:0.5px;">%s</h1>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding: 40px;">
                          <h2 style="color:#1f2937; font-size:20px; margin-top:0;">Bonjour %s,</h2>
                          <p style="color:#4b5563; font-size:15px; line-height:1.6;">
                            Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour en choisir un nouveau. Ce lien est valable 1 heure.
                          </p>

                          <table role="presentation" cellpadding="0" cellspacing="0" style="margin: 32px 0;">
                            <tr>
                              <td align="center" style="border-radius:6px; background-color:%s;">
                                <a href="%s" target="_blank" style="display:inline-block; padding: 14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:6px;">
                                  Réinitialiser mon mot de passe
                                </a>
                              </td>
                            </tr>
                          </table>

                          <p style="color:#9ca3af; font-size:13px; line-height:1.6;">
                            Si vous n'êtes pas à l'origine de cette demande, ignorez cet email : votre mot de passe restera inchangé.
                          </p>
                        </td>
                      </tr>

                      <tr>
                        <td style="background-color:#f9fafb; padding: 24px 40px; text-align:center; border-top:1px solid #e5e7eb;">
                          <p style="color:#9ca3af; font-size:12px; margin:0;">
                            © %s %s. Tous droits réservés.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                BRAND_COLOR, BRAND_NAME,
                prenom,
                BRAND_COLOR, resetLink,
                Year.now(), BRAND_NAME
        );
    }
}