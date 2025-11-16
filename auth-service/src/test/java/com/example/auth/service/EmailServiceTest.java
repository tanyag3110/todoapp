package com.example.auth.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;

import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void sendEmail_success_invokesSend() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);
        emailService.sendEmail("to@test", "subject", "<b>hi</b>");
        verify(mailSender).send(msg);
    }

    @Test
    void sendEmail_sendThrows_wrappedAsRuntimeException() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(msg);
        doThrow(new MailException("fail") {}).when(mailSender).send(msg);
        assertThatThrownBy(() -> emailService.sendEmail("to","s","b"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send mail");
    }
}
