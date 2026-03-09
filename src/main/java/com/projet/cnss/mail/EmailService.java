package com.projet.cnss.mail;

import com.projet.cnss.entity.EmailDetails;
import com.projet.cnss.entity.User;

public interface EmailService {
    void sendVerificationEmail(User user);
    String sendSimpleMail(String to, String subject, String text);
    String sendMailWithAttachment(EmailDetails details);
    public void sendPasswordResetEmail(User user, String resetToken);
}
