package com.label4002.blog.controller;

import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.RefreshTokenRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Profile("e2e")
@RestController
@RequestMapping("/api/testing")
public class E2ETestingController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public E2ETestingController(RefreshTokenRepository refreshTokenRepository,
                                PostRepository postRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/reset")
    @Transactional
    public ResetResponse reset(@RequestBody(required = false) ResetRequest request) {
        boolean includePosts = request == null || request.includePosts() == null || request.includePosts();
        boolean includeOtherUserPost = request == null
                || request.includeOtherUserPost() == null
                || request.includeOtherUserPost();

        refreshTokenRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123456"));
        admin = userRepository.save(admin);

        UserEntity other = new UserEntity();
        other.setUsername("other");
        other.setPassword(passwordEncoder.encode("other123456"));
        other = userRepository.save(other);

        Long adminPostId = null;
        Long otherPostId = null;

        if (includePosts) {
            PostEntity adminPost = new PostEntity();
            adminPost.setTitle("管理员示例文章");
            adminPost.setContent("管理员示例正文，用于E2E回归。");
            adminPost.setAuthor(admin);
            adminPost.setCreatedAt(LocalDateTime.now().minusHours(2));
            adminPost = postRepository.save(adminPost);
            adminPostId = adminPost.getId();

            if (includeOtherUserPost) {
                PostEntity otherPost = new PostEntity();
                otherPost.setTitle("其他用户文章");
                otherPost.setContent("其他用户正文，用于权限边界测试。");
                otherPost.setAuthor(other);
                otherPost.setCreatedAt(LocalDateTime.now().minusHours(1));
                otherPost = postRepository.save(otherPost);
                otherPostId = otherPost.getId();
            }
        }

        return new ResetResponse(admin.getId(), other.getId(), adminPostId, otherPostId);
    }

    public record ResetRequest(Boolean includePosts, Boolean includeOtherUserPost) {
    }

    public record ResetResponse(Long adminUserId, Long otherUserId, Long adminPostId, Long otherPostId) {
    }
}
