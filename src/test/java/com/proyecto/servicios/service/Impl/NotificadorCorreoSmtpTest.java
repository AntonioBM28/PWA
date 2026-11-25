package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CorreoNoEnviadoException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificadorCorreoSmtpTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final NotificadorCorreoSmtp notificador =
            new NotificadorCorreoSmtp(mailSender, "remitente@gmail.com", "PagosApp");

    @Test
    void enviaElCodigoAlUsuarioSinPonerloEnElAsunto() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        notificador.enviarCodigoVerificacion("maria@mail.com", "123456", Duration.ofMinutes(5));

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviado.capture());
        MimeMessage mensaje = enviado.getValue();
        assertEquals("maria@mail.com", ((InternetAddress) mensaje.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        InternetAddress remitente = (InternetAddress) mensaje.getFrom()[0];
        assertEquals("remitente@gmail.com", remitente.getAddress());
        assertEquals("PagosApp", remitente.getPersonal());
        assertEquals(NotificadorCorreoSmtp.ASUNTO, mensaje.getSubject());
        assertFalse(mensaje.getSubject().contains("123456"));
    }

    @Test
    void elCuerpoIncluyeElCodigoYLaVigencia() {
        String texto = NotificadorCorreoSmtp.textoPlano("123456", Duration.ofMinutes(5));
        String html = NotificadorCorreoSmtp.html("123456", Duration.ofMinutes(5));

        assertTrue(texto.contains("123456") && texto.contains("5 minutos"));
        assertTrue(html.contains("123456") && html.contains("5 minutos"));
    }

    @Test
    void siElServidorDeCorreoFalla_lanzaUnErrorEntendible() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("smtp caido")).when(mailSender).send(any(MimeMessage.class));

        CorreoNoEnviadoException ex = assertThrows(CorreoNoEnviadoException.class,
                () -> notificador.enviarCodigoVerificacion("maria@mail.com", "123456", Duration.ofMinutes(5)));

        assertFalse(ex.getMessage().contains("smtp"));
    }
}
