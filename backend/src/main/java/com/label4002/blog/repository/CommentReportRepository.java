package com.label4002.blog.repository;

import com.label4002.blog.entity.CommentReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReportEntity, Long> {

    Page<CommentReportEntity> findByStatusOrderByCreatedAtDesc(CommentReportEntity.ReportStatus status, Pageable pageable);

    Page<CommentReportEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CommentReportEntity> findByCommentId(Long commentId);

    List<CommentReportEntity> findByReporterId(Long reporterId);

    long countByStatus(CommentReportEntity.ReportStatus status);
}
