package com.demo.api.repository;

import com.demo.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @Query("select distinct u.avatar from User u where u.avatar is not null and u.avatar <> ''")
    List<String> findAllAvatar();
}
