package com.wallparisoft.ebill.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.wallparisoft.ebill.auth.util","com.wallparisoft.ebill.customer"})
public class EBillCustomerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EBillCustomerApplication.class, args);
	}

}
