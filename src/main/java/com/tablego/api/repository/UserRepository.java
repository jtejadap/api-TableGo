package com.tablego.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tablego.api.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
