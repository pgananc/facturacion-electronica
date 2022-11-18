package com.wallparisoft.repository;


import com.wallparisoft.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ILoginRepository extends JpaRepository<User, Long> {

	@Query("FROM User us where us.userName = :username and us.password= :password and us.status=true")
	User validateByUserName(@Param("username") String username,@Param("password") String password) throws Exception;

	
}
