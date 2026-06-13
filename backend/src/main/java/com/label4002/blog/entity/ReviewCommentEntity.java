package com.label4002.blog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_comments")
public class ReviewCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private ReviewRoundEntity round;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentPriority priority = CommentPriority.SUGGESTION;

    @Column(name = "paragraph_index")
    private Integer paragraphIndex;

    @Column(name = "position_start")
    private Integer positionStart;

    @Column(name = "position_end")
    private Integer positionEnd;

    @Column(name = "author_reply", columnDefinition = "TEXT")
    private String authorReply;

    @Column(name = "author_resolved", nullable = false)
    private boolean authorResolved = false;

    @Column(name = "author_replied_at")
    private LocalDateTime authorRepliedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReviewRoundEntity getRound() {
        return round;
    }

    public void setRound(ReviewRoundEntity round) {
        this.round = round;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CommentPriority getPriority() {
        return priority;
    }

    public void setPriority(CommentPriority priority) {
        this.priority = priority;
    }

    public Integer getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(Integer paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public Integer getPositionStart() {
        return positionStart;
    }

    public void setPositionStart(Integer positionStart) {
        this.positionStart = positionStart;
    }

    public Integer getPositionEnd() {
        return positionEnd;
    }

    public void setPositionEnd(Integer positionEnd) {
        this.positionEnd = positionEnd;
    }

    public String getAuthorReply() {
        return authorReply;
    }

    public void setAuthorReply(String authorReply) {
        this.authorReply = authorReply;
    }

    public boolean isAuthorResolved() {
        return authorResolved;
    }

    public void setAuthorResolved(boolean authorResolved) {
        this.authorResolved = authorResolved;
    }

    public LocalDateTime getAuthorRepliedAt() {
        return authorRepliedAt;
    }

    public void setAuthorRepliedAt(LocalDateTime authorRepliedAt) {
        this.authorRepliedAt = authorRepliedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
