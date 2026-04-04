package com.wallparisoft.ebill.product;

import com.wallparisoft.ebill.auth.util.config.SecurityConfig;
import com.wallparisoft.ebill.product.config.AuthUtilConfig;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Import({SecurityConfig.class, AuthUtilConfig.class})
@ComponentScan(
    basePackages = {"com.wallparisoft.ebill.product", "com.wallparisoft.ebill.utils"},
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmailUtil.class))
@SpringBootApplication
public class EBillProductApplication {

  public static void main(String[] args) {
    SpringApplication.run(EBillProductApplication.class, args);
  }
}
