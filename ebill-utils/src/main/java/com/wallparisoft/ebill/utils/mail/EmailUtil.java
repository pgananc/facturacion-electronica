package com.wallparisoft.ebill.utils.mail;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_003;

import com.wallparisoft.ebill.utils.dto.MailDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.exception.InternalException;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailUtil {
  private final JavaMailSender emailSender;

  public EmailUtil(JavaMailSender emailSender) {
    this.emailSender = emailSender;
  }

  public void sendMail(MailDto mail) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_003);
    try {
      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(
              message,
              MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
              StandardCharsets.UTF_8.name());
      helper.setTo(mail.getTo());
      helper.setText(mail.getTemplateHtml(), true);
      helper.setSubject(mail.getSubject());
      helper.setFrom(mail.getFrom());
      emailSender.send(message);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(mail.getTo()),
          RESPONSE,
          LEVEL_003);
    } catch (Exception e) {
      log.error("Error al enviar mail", e);
      throw new InternalException("Error al enviar mail", e);
    }
  }

  public void sendMailWithAttachment(MailDtoExt mail) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_003);

    try {
      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(
              message,
              MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
              StandardCharsets.UTF_8.name());
      helper.setTo(mail.getTo());
      helper.setText(mail.getTemplateHtml(), true);
      helper.setSubject(mail.getSubject());
      helper.setFrom(mail.getFrom());
      mail.getListAttachmentDtos()
          .forEach(
              a -> {
                try {
                  helper.addAttachment(a.getLabel(), new ByteArrayResource(a.getAttachment()));
                } catch (MessagingException e) {
                  log.error("Error al enviar adjunto", e);
                  throw new InternalException("Error al enviar mail", e);
                }
              });
      emailSender.send(message);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(mail.getTo()),
          RESPONSE,
          LEVEL_003);
    } catch (Exception e) {
      log.error("Error al enviar mail", e);
      throw new InternalException("Error al enviar mail", e);
    }
  }
}
