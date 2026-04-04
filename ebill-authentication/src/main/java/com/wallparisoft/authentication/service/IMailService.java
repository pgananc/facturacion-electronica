package com.wallparisoft.authentication.service;

public interface IMailService {

  public Integer sendMailCodeClient(String name, String mail, String tokenDuration, String token);
}
