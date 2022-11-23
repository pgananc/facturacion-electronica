package com.wallparisoft.repository;


import com.wallparisoft.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ILoginRepository extends JpaRepository<User, Long> {

	User findByUserNameAndPasswordAndStatus(
			@Param("username") String username,@Param("password") String password,
			@Param("status") Boolean status) throws Exception;

	
}
