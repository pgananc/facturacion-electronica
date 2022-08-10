package com.wallparisoft.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wallparisoft.model.Menu;

public interface IMenuRepository extends JpaRepository<Menu, Long>{
	
	@Query(value="select m.* from menu_rol mr inner join user_rol ur on ur.id_rol = mr.id_rol inner join menu m on m.id_menu = mr.id_menu inner join users u on u.id_user = ur.id_user where u.username = :name", nativeQuery = true)
	List<Object[]> findMenuByUser(@Param("name") String name);

}
