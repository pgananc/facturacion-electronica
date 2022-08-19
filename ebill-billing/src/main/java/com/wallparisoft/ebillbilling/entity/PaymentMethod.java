package com.wallparisoft.ebillbilling.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "payment_method")
@Data
public class PaymentMethod {

	@Id
	@SequenceGenerator(name = "payment_method_seq", sequenceName = "payment_method_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_method_seq")
	@Column(name = "id_payment_method")
	private Long idPaymentMethod;

	@Column(name = "creation_date", nullable = false)
	private LocalDateTime creationDate;

	@Column(name = "payment_method_type")
	private String paymentMethodType;

	@Column(name = "status", nullable = false)
	private boolean status;

}
