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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_comment_settings")
public class PostCommentSettingsEntity {

    public enum AccessRule {
        OPEN, SUBSCRIBERS_ONLY, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false, unique = true)
    private Long postId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_rule", nullable = false, length = 20)
    private AccessRule accessRule = AccessRule.OPEN;

    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval = false;

    @Column(name = "max_depth", nullable = false)
    private int maxDepth = 3;

    @Column(name = "max_length", nullable = false)
    private int maxLength = 2000;

    @Column(name = "allow_images", nullable = false)
    private boolean allowImages = true;

    @Column(name = "allow_code", nullable = false)
    private boolean allowCode = true;

    @Column(name = "allow_emojis", nullable = false)
    private boolean allowEmojis = true;

    @Column(name = "allow_mentions", nullable = false)
    private boolean allowMentions = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public PostEntity getPost() {
        return post;
    }

    public void setPost(PostEntity post) {
        this.post = post;
    }

    public AccessRule getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(AccessRule accessRule) {
        this.accessRule = accessRule;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean isAllowImages() {
        return allowImages;
    }

    public void setAllowImages(boolean allowImages) {
        this.allowImages = allowImages;
    }

    public boolean isAllowCode() {
        return allowCode;
    }

    public void setAllowCode(boolean allowCode) {
        this.allowCode = allowCode;
    }

    public boolean isAllowEmojis() {
        return allowEmojis;
    }

    public void setAllowEmojis(boolean allowEmojis) {
        this.allowEmojis = allowEmojis;
    }

    public boolean isAllowMentions() {
        return allowMentions;
    }

    public void setAllowMentions(boolean allowMentions) {
        this.allowMentions = allowMentions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
