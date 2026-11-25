package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CorreoNoEnviadoException;
import com.proyecto.servicios.service.NotificadorCorreo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envia los correos por SMTP (Gmail con contrasena de aplicacion). El codigo no se escribe en los
 * logs ni en el asunto, para que no aparezca en vistas previas de notificaciones.
 */
@Service
@Slf4j
public class NotificadorCorreoSmtp implements NotificadorCorreo {

    static final String ASUNTO = "Tu codigo de verificacion de PagosApp";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String nombreRemitente;

    public NotificadorCorreoSmtp(JavaMailSender mailSender,
                                 @Value("${spring.mail.username}") String remitente,
                                 @Value("${onboarding.correo.nombre-remitente}") String nombreRemitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    public void enviarCodigoVerificacion(String correo, String codigo, Duration vigencia) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(correo);
            helper.setSubject(ASUNTO);
            helper.setText(textoPlano(codigo, vigencia), html(codigo, vigencia));
            mailSender.send(mensaje);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            // Sin el detalle: puede incluir el servidor SMTP o el correo del usuario
            log.error("No se pudo enviar el codigo de verificacion: {}", e.getClass().getSimpleName());
            throw new CorreoNoEnviadoException();
        }
    }

    static String textoPlano(String codigo, Duration vigencia) {
        return "Tu codigo de verificacion es: " + codigo + "\n\n"
                + "Vence en " + vigencia.toMinutes() + " minutos y solo puede usarse una vez.\n"
                + "Si no intentaste iniciar sesion, ignora este correo y cambia tu contrasena.";
    }

    /** El codigo son solo digitos, asi que no requiere escapar HTML. */
    static String html(String codigo, Duration vigencia) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#1b1b1b">
                  <h2 style="color:#0b5c6b">PagosApp</h2>
                  <p>Usa este codigo para terminar de iniciar sesion:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0">%s</p>
                  <p>Vence en %d minutos y solo puede usarse una vez.</p>
                  <p style="color:#666;font-size:13px">Si no intentaste iniciar sesion, ignora este correo
                  y cambia tu contrasena. Nunca te pediremos este codigo por telefono.</p>
                </div>
                """.formatted(codigo, vigencia.toMinutes());
    }
}
