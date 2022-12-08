package com.wallparisoft.ebill.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.wallparisoft.ebill.auth.util","com.wallparisoft.ebill.product"})
public class EBillProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(EBillProductApplication.class, args);
	}

}
