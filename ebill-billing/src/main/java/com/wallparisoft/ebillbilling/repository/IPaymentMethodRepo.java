package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.PaymentMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentMethodRepo extends JpaRepository<PaymentMethod, Long> {
  Optional<PaymentMethod> findByPaymentMethodCode(String paymentMethodCode);
}
