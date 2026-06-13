package com.label4002.blog.entity;

import java.io.Serializable;
import java.util.Objects;

public class PostKeywordId implements Serializable {

    private Long post;
    private Long keyword;

    public PostKeywordId() {
    }

    public PostKeywordId(Long post, Long keyword) {
        this.post = post;
        this.keyword = keyword;
    }

    public Long getPost() {
        return post;
    }

    public void setPost(Long post) {
        this.post = post;
    }

    public Long getKeyword() {
        return keyword;
    }

    public void setKeyword(Long keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostKeywordId that = (PostKeywordId) o;
        return Objects.equals(post, that.post) && Objects.equals(keyword, that.keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(post, keyword);
    }
}
