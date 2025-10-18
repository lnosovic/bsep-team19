package com.example.publickeyinfrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    @Value("${spring.mail.username}")
    private String fromEmail;
    private JavaMailSender javaMailSender;
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    @Async
    public void sendNotificaitionAsync(String to, String subject, String body){
        System.out.println("Async metoda se izvrsava u drugom Threadu u odnosu na prihvaceni zahtev. Thread id: " + Thread.currentThread().getId());
        System.out.println("Slanje emaila...");
        logger.info("Attempting to send email.",
                kv("recipient", to),
                kv("subject", subject),
                kv("threadId", Thread.currentThread().getId()));
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setFrom(fromEmail.trim());
            mail.setSubject(subject);
            mail.setText(body);
            javaMailSender.send(mail);

            logger.info("Email successfully sent.",
                    kv("recipient", to),
                    kv("outcome", "SUCCESS"));
        } catch (Exception e) {
            logger.error("Failed to send email to '{}'.", to,
                    kv("recipient", to),
                    kv("outcome", "FAILURE"),
                    kv("reason", e.getMessage()));
        }
    }
}
