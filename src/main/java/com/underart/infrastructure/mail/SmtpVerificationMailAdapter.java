package com.underart.infrastructure.mail;


import com.underart.domain.exception.VerificationMailException;
import com.underart.domain.port.out.VerificationMailPort;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Component
public class SmtpVerificationMailAdapter implements VerificationMailPort {


    private final JavaMailSender mail;
    private static final String SUBJECT = "Underart verification code";

    private final String sender;
    private final String brevoApiKey;
    private final RestClient brevo;


    public SmtpVerificationMailAdapter(
            JavaMailSender mail,
            @Value("${registration.mail-from}") String sender,
            @Value("${BREVO_API_KEY:}") String brevoApiKey) {
        this.mail = mail;
        this.sender = sender;
        this.brevoApiKey = brevoApiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.brevo = RestClient.builder().requestFactory(factory).build();
    }


    @Override
    public void sendCode(String email, String code) {
        String body = "Your Underart verification code is " + code + ". It expires in 10 minutes.";
        if (!brevoApiKey.isBlank()) {
            sendWithBrevo(email, body);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(body);
        try {
            mail.send(message);
        } catch (MailException ex) {
            throw new VerificationMailException(ex);
        }
    }

    private void sendWithBrevo(String email, String body) {
        try {
            brevo.post()
                    .uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sender", Map.of("email", sender, "name", "Underart"),
                            "to", List.of(Map.of("email", email)),
                            "subject", SUBJECT,
                            "textContent", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new VerificationMailException(ex);
        }
    }
}
