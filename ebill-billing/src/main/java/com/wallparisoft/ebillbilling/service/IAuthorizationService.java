package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;

public interface IAuthorizationService extends ICRUD<AuthorizationSri> {

  AuthorizationSri saveAuthorization(AuthorizationSri authorization);
}
