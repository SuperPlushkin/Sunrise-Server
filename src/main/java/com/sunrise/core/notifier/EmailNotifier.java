package com.sunrise.core.notifier;

import com.sunrise.core.dataservice.type.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailNotifier {

    @Value("${app.mail.mail-address}")
    private String mailAddress;
    @Value("${app.mail.base-url}")
    private String baseUrl;

    private final JavaMailSender mailSender;

    @Async // Пока что почта не гарантирована
    public void sendVerificationTokenMail(String to, TokenType tokenType, String token) {

        String subject;
        String body;
        String confirmUrl;

        switch (tokenType) {
            case REGISTRATION:
                subject = "Подтверждение регистрации на Sunrise Messenger";
                confirmUrl = baseUrl + "/auth/confirm-registration?token=" + token;
                body = String.format("""
                Здравствуйте!
                
                Для подтверждения регистрации перейдите по ссылке:
                %s
                
                Ссылка действительна 24 часа.
                """, confirmUrl);
                break;
            case EMAIL_UPDATE:
                subject = "Подтверждение смены email на Sunrise Messenger";
                confirmUrl = baseUrl + "/auth/confirm-email-update.html?token=" + token;
                body = String.format("""
                Здравствуйте!
                
                Для подтверждения смены email перейдите по ссылке и введите новый email:
                %s
                
                Ссылка действительна 24 часа.
                """, confirmUrl);
                break;
            case PASSWORD_UPDATE:
                subject = "Сброс пароля на Sunrise Messenger";
                confirmUrl = baseUrl + "/auth/confirm-password-reset?token=" + token;
                body = String.format("""
                Здравствуйте!
                
                Для сброса пароля перейдите по ссылке и введите новый пароль:
                %s
                
                Ссылка действительна 1 час.
                """, confirmUrl);
                break;
            default:
                throw new IllegalArgumentException("Unknown token type");
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailAddress);
        email.setTo(to);
        email.setSubject(subject);
        email.setText(body);
        mailSender.send(email);
    }
}
