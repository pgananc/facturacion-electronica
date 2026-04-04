package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IRoleRepository extends JpaRepository<Role, Long> {

  List<Role> findByStatusAndNameNotOrderByNameAsc(
      @Param("status") boolean status, @Param("name") String name);

  @Query(
      """
                SELECT r
                FROM UserCompanyRole ucr
                JOIN ucr.role r
                JOIN ucr.userCompany uc
                JOIN uc.user u
                WHERE u.idUser = :idUser
                  AND ucr.status = true
            """)
  List<Role> findActiveRolesByUserId(@Param("idUser") Long idUser);

  @Query(
      """
                SELECT r
                FROM UserCompanyRole ucr
                JOIN ucr.role r
                JOIN ucr.userCompany uc
                JOIN uc.user u
                WHERE u.idUser = :idUser
                  AND uc.idCompany = :idCompany
                  AND ucr.status = true
            """)
  List<Role> findActiveRolesByUserIdAndCompanyId(
      @Param("idUser") Long idUser, @Param("idCompany") Long idCompany);

  List<Role> findByStatusOrderByNameAsc(@Param("status") boolean status);
}
