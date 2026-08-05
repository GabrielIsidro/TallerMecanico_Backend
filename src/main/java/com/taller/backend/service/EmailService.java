package com.taller.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Spring Boot inyecta automáticamente el motor de correos que configuramos en el application.properties
    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailBienvenida(String destinatario, String nombreTaller, String passwordTemporal) {
        
        SimpleMailMessage mensaje = new SimpleMailMessage();
        
        mensaje.setTo(destinatario);
        mensaje.setSubject("¡Bienvenido a TuTaller SaaS! - Accesos a tu cuenta");
        
        String texto = "Hola,\n\n"
                + "Tu cuenta para administrar el taller '" + nombreTaller + "' ha sido creada con éxito en nuestra plataforma.\n\n"
                + "Para ingresar al sistema, utiliza las siguientes credenciales:\n\n"
                + "Email: " + destinatario + "\n"
                + "Contraseña temporal: " + passwordTemporal + "\n\n"
                + "Por cuestiones de seguridad, te recomendamos cambiar esta contraseña apenas inicies sesión por primera vez.\n\n"
                + "¡Éxitos con las reparaciones!\n"
                + "El equipo de TuTaller";
                
        mensaje.setText(texto);
        
        // ¡Le damos al botón de enviar!
        mailSender.send(mensaje);
    }

    public void enviarEmail(String destinatario, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        mailSender.send(mensaje);
    }
}