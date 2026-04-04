package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Guide;

public interface GuideEmailService {

  Integer sendAuthorizedGuide(
      String recipientEmail,
      Guide guide,
      GuideDto guideDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes);
}
