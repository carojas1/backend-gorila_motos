package com.projectBackend.GMotors.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configura el envío de correos por SMTP (Gmail) SIN necesidad de dominio.
 *
 * La "contraseña de aplicación" de Google se muestra con espacios para leerla
 * (ej. "nexz nvhf pndr hpf"), pero el servidor SMTP la necesita SIN espacios.
 * Aquí los quitamos automáticamente para que el valor pegado en Render funcione
 * tal cual, con o sin espacios.
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}")            int port,
            @Value("${spring.mail.username:}")           String username,
            @Value("${spring.mail.password:}")           String password) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username == null ? "" : username.trim());
        // Quita TODOS los espacios de la contraseña de aplicación
        sender.setPassword(password == null ? "" : password.replaceAll("\\s+", ""));
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
