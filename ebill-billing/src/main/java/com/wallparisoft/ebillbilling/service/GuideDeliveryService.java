package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Guide;

public interface GuideDeliveryService {

  void deliverAuthorizedGuide(GuideDto guideDto, Guide guide, AuthorizationSri authorizationSri);
}
