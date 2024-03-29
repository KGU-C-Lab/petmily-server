package com.clab.securecoding.repository;

import com.clab.securecoding.type.entity.User;
import com.clab.securecoding.type.etc.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId")
    boolean existsByUserId(String userId);

    boolean existsByContact(String contact);

    List<User> findAllByRole(Role role);

}
