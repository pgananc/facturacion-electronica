package com.facturacion.electronica.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.facturacion.electronica.modelo.ComprobanteVenta;

public interface IComprobanteVentaRepo extends JpaRepository<ComprobanteVenta, Integer> {

}
