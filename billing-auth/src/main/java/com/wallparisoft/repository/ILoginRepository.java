package com.wallparisoft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wallparisoft.model.User;

public interface ILoginRepository extends JpaRepository<User, Long> {

	@Query("FROM User us where us.username = :username and us.password= :password")
	User validateByUserName(@Param("username") String username,@Param("password") String password) throws Exception;

	
}
