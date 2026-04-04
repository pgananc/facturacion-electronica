package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ILoginRepository extends JpaRepository<User, Long> {

  Optional<User> findByUserNameAndStatusTrue(String userName);
}
