package com.wallparisoft.ebillbilling.config;

import com.wallparisoft.ebill.utils.mail.EmailUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

  @Bean
  public EmailUtil emailUtil(JavaMailSender javaMailSender) {
    return new EmailUtil(javaMailSender);
  }
}
