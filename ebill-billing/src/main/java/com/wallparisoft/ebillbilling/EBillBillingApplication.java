package com.wallparisoft.ebillbilling;

import com.wallparisoft.ebill.utils.mail.EmailUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableRetry
@ComponentScan(
    basePackages = {
      "com.wallparisoft.ebill.auth.util",
      "com.wallparisoft.ebillbilling",
      "com.wallparisoft.ebill.utils"
    },
    excludeFilters = {
      @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmailUtil.class)
    })
@EnableScheduling
@EnableAsync
public class EBillBillingApplication {

  public static void main(String[] args) {
    SpringApplication.run(EBillBillingApplication.class, args);
  }
}
