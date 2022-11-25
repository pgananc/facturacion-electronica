package com.wallparisoft.ebill.utils.mail;


import com.wallparisoft.ebill.utils.dto.MailDto;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Component
public class EmailUtil {
	private final JavaMailSender emailSender;


	public EmailUtil(JavaMailSender emailSender) {
		this.emailSender = emailSender;
	}

	public void sendMail(MailDto mail) {
		try {
			MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(mail.getTo());
            helper.setText(mail.getTemplateHtml(), true);
            helper.setSubject(mail.getSubject());
            //helper.setFrom(mail.getFrom());
            emailSender.send(message);

		}catch(Exception e) {
			throw new RuntimeException(e);
		}
		
	}
}
