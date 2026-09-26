package com.taller.backend.core.service;

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
        String asunto = "¡Bienvenido a TuTaller SaaS! - Accesos a tu cuenta";
        String texto = "Hola,\n\n"
                + "Tu cuenta para administrar el taller '" + nombreTaller + "' ha sido creada con éxito en nuestra plataforma.\n\n"
                + "Para ingresar al sistema, utiliza las siguientes credenciales:\n\n"
                + "Email: " + destinatario + "\n"
                + "Contraseña temporal: " + passwordTemporal + "\n\n"
                + "Por cuestiones de seguridad, te recomendamos cambiar esta contraseña apenas inicies sesión por primera vez.\n\n"
                + "¡Éxitos con las reparaciones!\n"
                + "El equipo de TuTaller";

        enviarEmail(destinatario, asunto, texto);
    }

    public void enviarCodigoRecuperacion(String destinatario, String codigo) {
        String asunto = "TuTaller SaaS - Código de Recuperación de Contraseña";
        String texto = "Hola,\n\n"
                + "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en TuTaller SaaS.\n\n"
                + "Tu código de verificación de 6 dígitos es:\n\n"
                + "       " + codigo + "\n\n"
                + "Este código es válido por 15 minutos. Si tú no solicitaste este cambio, puedes ignorar este mensaje.\n\n"
                + "Saludos,\n"
                + "El equipo de TuTaller SaaS";

        enviarEmail(destinatario, asunto, texto);
    }

    public void enviarEmail(String destinatario, String asunto, String texto) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("⚠️ [EmailService] No se pudo enviar el correo a '" + destinatario + "'. Detalle: " + e.getMessage());
            System.out.println("ℹ️ [EmailService LOCAL FALLBACK] Asunto: " + asunto);
            System.out.println("ℹ️ [EmailService LOCAL FALLBACK] Mensaje:\n" + texto);
        }
    }
}
