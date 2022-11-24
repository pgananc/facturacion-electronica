package com.wallparisoft.service;

import java.util.List;

/**
 * CRUD.
 * 
 * @author PABI1
 *
 * @param <T>
 */
public interface ICRUD<T> {

	T save(T entity);

	List<T> findAll();

	T findById(Long id);

}
