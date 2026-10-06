package com.securecms.repository;

import com.securecms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // @EntityGraph = "also JOIN the role in the same SQL query" (avoids a second SELECT)
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
