package com.label4002.blog.repository;

import com.label4002.blog.entity.KeywordEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostKeywordEntity;
import com.label4002.blog.entity.PostKeywordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostKeywordRepository extends JpaRepository<PostKeywordEntity, PostKeywordId> {

    @Query("SELECT pk.keyword.id FROM PostKeywordEntity pk WHERE pk.post.id = :postId")
    List<Long> findKeywordIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT pk.keyword FROM PostKeywordEntity pk WHERE pk.post.id = :postId")
    List<KeywordEntity> findKeywordsByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM PostKeywordEntity pk WHERE pk.post.id = :postId AND pk.keyword.id = :keywordId")
    void deleteByPostIdAndKeywordId(@Param("postId") Long postId, @Param("keywordId") Long keywordId);

    @Modifying
    @Query("DELETE FROM PostKeywordEntity pk WHERE pk.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Query(value = "INSERT IGNORE INTO post_keywords (post_id, keyword_id) VALUES (:postId, :keywordId)", nativeQuery = true)
    void insertPostKeyword(@Param("postId") Long postId, @Param("keywordId") Long keywordId);

    @Query("SELECT pk.post FROM PostKeywordEntity pk WHERE pk.keyword.id = :keywordId")
    List<PostEntity> findByKeywordId(@Param("keywordId") Long keywordId);
}
