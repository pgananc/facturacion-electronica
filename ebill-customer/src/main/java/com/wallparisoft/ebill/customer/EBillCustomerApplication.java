package com.wallparisoft.ebill.customer;

import com.wallparisoft.ebill.utils.mail.EmailUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = {
      "com.wallparisoft.ebill.auth.util",
      "com.wallparisoft.ebill.customer",
      "com.wallparisoft.ebill.utils"
    },
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmailUtil.class))
public class EBillCustomerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EBillCustomerApplication.class, args);
  }
}
