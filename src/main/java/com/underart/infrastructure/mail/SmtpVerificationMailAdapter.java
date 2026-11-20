package com.underart.infrastructure.mail;

import com.underart.domain.exception.VerificationMailException;
import com.underart.domain.port.out.VerificationMailPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpVerificationMailAdapter implements VerificationMailPort {

    private final JavaMailSender mail;
    private final String sender;

    public SmtpVerificationMailAdapter(JavaMailSender mail, @Value("${registration.mail-from}") String sender) {
        this.mail = mail;
        this.sender = sender;
    }

    @Override
    public void sendCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Underart verification code");
        message.setText("Your Underart verification code is " + code + ". It expires in 10 minutes.");
        try {
            mail.send(message);
        } catch (MailException ex) {
            throw new VerificationMailException(ex);
        }
    }
}
