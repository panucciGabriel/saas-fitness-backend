package com.meuprojeto.saas.feature.password;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        // Coloque aqui o seu e-mail dev
        message.setFrom("seue-mail.dev@gmail.com");
        message.setTo(toEmail);
        message.setSubject("TreinoDex - Recuperação de Senha");
        message.setText("Olá!\n\nClique no link para redefinir a sua senha:\n" + resetUrl);

        try {
            // 🛑 VAMOS TENTAR ENVIAR O E-MAIL
            mailSender.send(message);
            System.out.println("✅ E-mail enviado com sucesso para: " + toEmail);
        } catch (Exception e) {
            // ⚠️ SE O GOOGLE BLOQUEAR, NÓS MOSTRAMOS O LINK NO TERMINAL E O SISTEMA NÃO QUEBRA!
            System.out.println("❌ O Google bloqueou o envio do e-mail. Mas não faz mal!");
            System.out.println("🔗 COPIE ESTE LINK E COLE NO NAVEGADOR PARA TESTAR: " + resetUrl);
        }
    }
}