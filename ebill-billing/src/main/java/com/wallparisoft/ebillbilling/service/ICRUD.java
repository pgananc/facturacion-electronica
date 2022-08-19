package com.wallparisoft.ebillbilling.service;


import java.util.List;

public interface ICRUD<T> {

	T register(T obj);
	T modify(T obj);
	List<T> list();
	T getById(Integer id);
	boolean delete(Integer id);
}

