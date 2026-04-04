package com.wallparisoft.ebillbilling.controller;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.entity.PaymentMethod;
import com.wallparisoft.ebillbilling.repository.IPaymentMethodRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment-method")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class PaymentMethodController {

  final IPaymentMethodRepo paymentMethodRepo;

  @GetMapping
  public ResponseEntity<List<PaymentMethod>> getAllPaymentMethods() {
    log.info("Fetching all payment methods");
    List<PaymentMethod> paymentMethods = paymentMethodRepo.findAll();
    log.info("Found {} payment methods", paymentMethods.size());
    return new ResponseEntity<>(paymentMethods, HttpStatus.OK);
  }
}
