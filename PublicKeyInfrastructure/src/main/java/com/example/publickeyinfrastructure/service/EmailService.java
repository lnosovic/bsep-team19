package com.example.publickeyinfrastructure.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    @Async
    public void sendNotificaitionAsync(String to, String subject){
        System.out.println("Async metoda se izvrsava u drugom Threadu u odnosu na prihvaceni zahtev. Thread id: " + Thread.currentThread().getId());
        //Simulacija duze aktivnosti da bi se uocila razlika
        System.out.println("Slanje emaila...");

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setFrom("Aktivacija naloga");
        mail.setSubject("Primer slanja emaila pomoću asinhronog Spring taska");
        mail.setText("Pozdrav");
        javaMailSender.send(mail);

        System.out.println("Email poslat!");
    }
}
