package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentDto {

  Long idPayment;
  Long idPaymentMethod;
  String paymentMethod; // Nombre de la forma de pago
  String paymentType; // Tipo (ej: "SIN UTILIZACION DEL SISTEMA FINANCIERO")
  String paymentMethodCode; // Código SRI (ej: "01")
  BigDecimal total; // Monto de esta forma de pago
  BigDecimal value; // Alias para total (compatibilidad frontend)
  Integer term; // Plazo
  String timeUnit; // Unidad de tiempo (ej: "DÍAS")
  String time; // Alias para timeUnit (compatibilidad frontend)
}
