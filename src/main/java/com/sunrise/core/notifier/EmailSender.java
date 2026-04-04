package com.sunrise.core.notifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {

    @Value("${app.mail.mail-address}")
    private String mailAddress;
    @Value("${app.mail.base-url}")
    private String baseUrl;

    private final JavaMailSender mailSender;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Пока что почта не гарантирована
    @Async
    public void sendVerificationEmail(String to, String token) {

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailAddress);
        email.setTo(to);
        email.setSubject("Подтверждение регистрации на Sunrise Messenger");
        email.setText(String.format(
            """
            Здравствуйте!
            
            Для подтверждения регистрации перейдите по ссылке:
            %s/auth/confirm?type=email_confirmation&token=%s
            
            Ссылка действительна 24 часа.
            """,
            baseUrl, token
        ));

        mailSender.send(email);
    }
}
