package com.wallparisoft.ebill.customer.service;


import java.util.List;

public interface ICRUD<T> {

	T save(T entity);

	T update(T entity, Long id);

	List<T> findAll();

	T findById(Long id);

}

