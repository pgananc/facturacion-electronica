package com.wallparisoft.facturacion.electronica.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wallparisoft.facturacion.electronica.modelo.ComprobanteVenta;

public interface IComprobanteVentaRepo extends JpaRepository<ComprobanteVenta, Long> {

}
