package com.wallparisoft.ebill.customer.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.ebill.customer.entity.ComprobanteVenta;
import com.wallparisoft.ebill.customer.repository.IComprobanteVentaRepo;
import com.wallparisoft.ebill.customer.service.IComprobanteVentaServicio;

@Service
public class ComprobanteVentaServicioImpl implements IComprobanteVentaServicio {

	@Autowired
	private IComprobanteVentaRepo repo;

	@Override
	public ComprobanteVenta registrar(ComprobanteVenta obj) {
		return repo.save(obj);
	}

	@Override
	public ComprobanteVenta modificar(ComprobanteVenta obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ComprobanteVenta> listar() {
		return repo.findAll();
	}

	@Override
	public ComprobanteVenta leerPorId(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean eliminar(Integer id) {
		// TODO Auto-generated method stub
		return false;
	}

}
