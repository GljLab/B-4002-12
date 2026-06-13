package com.label4002.blog.repository;

import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    long countByRole(UserRole role);

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByRoleAndEnabledTrue(UserRole role);
}
