package com.wallparisoft.repository;

import com.wallparisoft.entity.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IMenuRepository extends JpaRepository<Menu, Long>{
	
	@Query(
			value="select m.* from menu_role mr " +
					"inner join user_role ur on ur.id_role = mr.id_role and ur.status=true " +
					"inner join menu m on m.id_menu = mr.id_menu and m.status=true " +
					"inner join users u on u.id_user = ur.id_user " +
					"where u.username = :name and mr.status=true", nativeQuery = true)
	List<Object[]> findMenuByUser(@Param("name") String name);

}
