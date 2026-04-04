package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IInvoicePaymentRepo extends JpaRepository<InvoicePayment, Long> {
  // Métodos personalizados si son necesarios
}
