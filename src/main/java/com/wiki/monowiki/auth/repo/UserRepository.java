package com.wiki.monowiki.auth.repo;

import java.util.Optional;

import com.wiki.monowiki.auth.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
}
