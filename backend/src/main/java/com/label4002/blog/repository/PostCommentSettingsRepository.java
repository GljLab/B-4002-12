package com.label4002.blog.repository;

import com.label4002.blog.entity.PostCommentSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCommentSettingsRepository extends JpaRepository<PostCommentSettingsEntity, Long> {

    Optional<PostCommentSettingsEntity> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    boolean existsByPostId(Long postId);
}
