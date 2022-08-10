package com.wallparisoft.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wallparisoft.model.User;

public interface IUserRepository extends JpaRepository<User, Integer> {

	
	User findOneByUsername(String username);
}
