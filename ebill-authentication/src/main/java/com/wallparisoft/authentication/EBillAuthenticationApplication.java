package com.wallparisoft.authentication;

import com.wallparisoft.authentication.config.AuthUtilConfig;
import com.wallparisoft.ebill.auth.util.config.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"com.wallparisoft.authentication", "com.wallparisoft.ebill.utils"})
@Import({SecurityConfig.class, AuthUtilConfig.class})
@EnableFeignClients(basePackages = "com.wallparisoft.authentication.client")
@EnableScheduling
public class EBillAuthenticationApplication {

  public static void main(String[] args) {
    SpringApplication.run(EBillAuthenticationApplication.class, args);
  }
}
