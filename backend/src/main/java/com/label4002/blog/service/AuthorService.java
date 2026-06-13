package com.label4002.blog.service;

import com.label4002.blog.dto.AuthorDTO;
import com.label4002.blog.dto.AuthorPublicDTO;
import com.label4002.blog.dto.ChangePasswordRequest;
import com.label4002.blog.dto.CreateAuthorRequest;
import com.label4002.blog.dto.UpdateAuthorProfileRequest;
import com.label4002.blog.dto.UpdateAuthorRequest;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.PostStatus;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.entity.UserRole;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthorService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthorService(UserRepository userRepository,
                         PostRepository postRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthorDTO createAuthor(CreateAuthorRequest request) {
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.AUTHOR);
        user.setNickname(request.nickname());
        user.setAvatarUrl(request.avatarUrl());
        user.setBio(request.bio());
        user.setEnabled(true);

        UserEntity saved = userRepository.save(user);
        return toAuthorDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AuthorDTO> listAuthors() {
        return userRepository.findByRole(UserRole.AUTHOR).stream()
                .map(this::toAuthorDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthorDTO getAuthor(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        return toAuthorDTO(user);
    }

    @Transactional
    public AuthorDTO updateAuthor(Long id, UpdateAuthorRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        UserEntity saved = userRepository.save(user);
        return toAuthorDTO(saved);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginAttempts(0);
        userRepository.save(user);
    }

    @Transactional
    public void disableAuthor(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void enableAuthor(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        user.setEnabled(true);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteAuthor(Long id, Long transferToAuthorId) {
        UserEntity author = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("作者不存在"));

        long publishedCount = postRepository.countByAuthorIdAndStatus(id, PostStatus.PUBLISHED);

        if (publishedCount > 0 && transferToAuthorId == null) {
            throw new BadRequestException("该作者有已发布的文章，必须指定转移目标作者");
        }

        if (transferToAuthorId != null) {
            UserEntity target = userRepository.findById(transferToAuthorId)
                    .orElseThrow(() -> new NotFoundException("目标作者不存在"));
            postRepository.transferAuthor(id, transferToAuthorId);
        } else {
            List<PostEntity> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(id);
            for (PostEntity post : posts) {
                if (post.getStatus() == PostStatus.PUBLISHED) {
                    post.setStatus(PostStatus.DRAFT);
                    postRepository.save(post);
                } else if (post.getStatus() == PostStatus.DRAFT || post.getStatus() == PostStatus.REJECTED) {
                    postRepository.delete(post);
                }
            }
        }

        userRepository.delete(author);
    }

    @Transactional
    public AuthorDTO updateProfile(Long userId, UpdateAuthorProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        UserEntity saved = userRepository.save(user);
        return toAuthorDTO(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("当前密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthorPublicDTO getPublicProfile(Long authorId) {
        UserEntity user = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        return toPublicDTO(user);
    }

    @Transactional(readOnly = true)
    public List<AuthorPublicDTO> listPublicAuthors() {
        return userRepository.findByRoleAndEnabledTrue(UserRole.AUTHOR).stream()
                .map(this::toPublicDTO)
                .toList();
    }

    private AuthorDTO toAuthorDTO(UserEntity user) {
        long postCount = postRepository.countByAuthorId(user.getId());
        return new AuthorDTO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.isEnabled(),
                user.getRole().name(),
                null,
                postCount,
                user.getLastLoginAt()
        );
    }

    private AuthorPublicDTO toPublicDTO(UserEntity user) {
        long postCount = postRepository.countByAuthorIdAndStatus(user.getId(), PostStatus.PUBLISHED);
        return new AuthorPublicDTO(
                user.getId(),
                user.getNickname(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getBio(),
                postCount
        );
    }
}
