package com.wallparisoft.ebill.utils.mail;


import com.wallparisoft.ebill.utils.dto.MailDto;
import com.wallparisoft.ebill.utils.exception.InternalException;
import com.wallparisoft.ebill.utils.log.EventLog;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_003;

@Component
@Log4j2
public class EmailUtil {
    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String accountMail;

    public EmailUtil(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendMail(MailDto mail) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(mail.getTo());
            helper.setText(mail.getTemplateHtml(), true);
            helper.setSubject(mail.getSubject());
            helper.setFrom(accountMail);
            emailSender.send(message);

            log.debug(EventLog.builder()
                    .service(traceElement.getClassName())
                    .method(traceElement.getMethodName())
                    .information(mail.getTo())
                    .eventType(RESPONSE.name())
                    .level(LEVEL_003.name())
                    .build());
        } catch (Exception e) {
            log.error("Error al enviar mail", e);
            throw new InternalException("Error al enviar mail", e);
        }

    }
}
