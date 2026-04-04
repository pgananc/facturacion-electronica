package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.entity.Params;
import com.wallparisoft.authentication.repository.IParamsRepository;
import com.wallparisoft.authentication.service.IMailService;
import com.wallparisoft.ebill.utils.dto.MailDto;
import com.wallparisoft.ebill.utils.enums.ParmsAuthEnum;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService implements IMailService {

  final IParamsRepository paramsRepository;
  final EmailUtil emailUtil;

  @Value("${spring.mail.username}")
  private String mailNotification;

  public MailService(IParamsRepository paramsRepository, EmailUtil emailUtil) {
    this.paramsRepository = paramsRepository;
    // TODO Auto-generated constructor stub
    this.emailUtil = emailUtil;
  }

  @Override
  @Transactional
  public Integer sendMailCodeClient(String name, String mail, String tokenDuration, String token) {
    int result = 0;
    try {
      Params paramsMail =
          paramsRepository.findParamsByCodeParam(
              ParmsAuthEnum.COD004_MAIL_TEMPLATE_CODE_CLIENT.getCode());
      String html = paramsMail.getValueParam();
      html =
          html.replace("$${user}", name)
              .replace("$${tokenDuration}", tokenDuration)
              .replace("$${token}", token)
              .replace("$S{year}", String.valueOf(LocalDateTime.now().getYear()));
      MailDto mailDto =
          MailDto.builder()
              .from(mailNotification)
              .to(mail)
              .subject("ACCESO CONSULTA DE FACTURAS - WALLPARISOFT")
              .templateHtml(html)
              .build();
      emailUtil.sendMail(mailDto);
      result = 1;
      return result;
    } catch (Exception e) {
      return 0;
    }
  }
}
