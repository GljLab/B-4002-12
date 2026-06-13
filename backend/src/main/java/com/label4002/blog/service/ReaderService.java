package com.label4002.blog.service;

import com.label4002.blog.dto.CaptchaDTO;
import com.label4002.blog.dto.CommentDTO;
import com.label4002.blog.dto.FavoriteDetailDTO;
import com.label4002.blog.dto.ReadHistoryDetailDTO;
import com.label4002.blog.dto.ReaderProfileDTO;
import com.label4002.blog.dto.ReaderPublicProfileDTO;
import com.label4002.blog.dto.RegisterReaderRequest;
import com.label4002.blog.dto.ResetPasswordRequest;
import com.label4002.blog.dto.SecurityQuestionDTO;
import com.label4002.blog.dto.UpdateReaderProfileRequest;
import com.label4002.blog.entity.CommentEntity;
import com.label4002.blog.entity.FavoriteEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.ReadHistoryEntity;
import com.label4002.blog.entity.RegistrationRateLimitEntity;
import com.label4002.blog.entity.SubscriptionEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.entity.UserRole;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.ForbiddenException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.CommentRepository;
import com.label4002.blog.repository.FavoriteRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.ReadHistoryRepository;
import com.label4002.blog.repository.RegistrationRateLimitRepository;
import com.label4002.blog.repository.SubscriptionRepository;
import com.label4002.blog.repository.UserRepository;
import com.label4002.blog.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReaderService {

    private static final int MAX_REGISTRATIONS_PER_IP_PER_HOUR = 5;
    private static final int[] LEVEL_THRESHOLDS = {0, 50, 150, 350, 700, 1200, 2000, 3200, 5000, 7500, 10000};

    private final UserRepository userRepository;
    private final ReadHistoryRepository readHistoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RegistrationRateLimitRepository registrationRateLimitRepository;
    private final PostRepository postRepository;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;

    public ReaderService(UserRepository userRepository,
                         ReadHistoryRepository readHistoryRepository,
                         FavoriteRepository favoriteRepository,
                         CommentRepository commentRepository,
                         SubscriptionRepository subscriptionRepository,
                         RegistrationRateLimitRepository registrationRateLimitRepository,
                         PostRepository postRepository,
                         CaptchaService captchaService,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.readHistoryRepository = readHistoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.registrationRateLimitRepository = registrationRateLimitRepository;
        this.postRepository = postRepository;
        this.captchaService = captchaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ReaderProfileDTO register(RegisterReaderRequest request, String clientIp) {
        if (!captchaService.validateCaptcha(request.captchaKey(), request.captchaCode())) {
            throw new BadRequestException("验证码错误或已过期");
        }

        String ipHash = captchaService.hashIp(clientIp);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long ipCount = registrationRateLimitRepository.countByIpHashAndCreatedAtAfter(ipHash, oneHourAgo);
        if (ipCount >= MAX_REGISTRATIONS_PER_IP_PER_HOUR) {
            throw new BadRequestException("该IP注册过于频繁，请稍后再试");
        }

        userRepository.findByUsername(request.username()).ifPresent(u -> {
            throw new BadRequestException("用户名已被占用");
        });

        RegistrationRateLimitEntity rateLimit = new RegistrationRateLimitEntity();
        rateLimit.setIpHash(ipHash);
        rateLimit.setCreatedAt(LocalDateTime.now());
        registrationRateLimitRepository.save(rateLimit);

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.READER);
        user.setNickname(request.nickname());
        user.setEnabled(true);
        user.setReaderLevel(1);
        user.setReaderExp(0);
        user.setSecurityQuestion(request.securityQuestion());
        user.setSecurityAnswer(request.securityAnswer() != null ? passwordEncoder.encode(request.securityAnswer()) : null);
        user.setShowFootprint(true);
        user.setStreakDays(0);
        user.setLastActiveDate(LocalDate.now());
        userRepository.save(user);

        return buildReaderProfile(user);
    }

    public CaptchaDTO generateCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return new CaptchaDTO(result.key(), result.svgDataUrl());
    }

    @Transactional
    public ReaderProfileDTO updateProfile(AppUserPrincipal principal, UpdateReaderProfileRequest request) {
        UserEntity user = findReaderOrThrow(principal.getId());

        if (request.nickname() != null) {
            user.setNickname(request.nickname().isBlank() ? null : request.nickname());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio());
        }
        if (request.showFootprint() != null) {
            user.setShowFootprint(request.showFootprint());
        }

        userRepository.save(user);
        return buildReaderProfile(user);
    }

    @Transactional(readOnly = true)
    public ReaderProfileDTO getMyProfile(AppUserPrincipal principal) {
        UserEntity user = findReaderOrThrow(principal.getId());
        return buildReaderProfile(user);
    }

    @Transactional(readOnly = true)
    public ReaderPublicProfileDTO getPublicProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getRole() != UserRole.READER) {
            throw new NotFoundException("读者不存在");
        }

        long readCount = readHistoryRepository.countByUserId(userId);
        long favCount = favoriteRepository.countByUserId(userId);
        long commentCount = commentRepository.countByUserId(userId);
        long subCount = subscriptionRepository.countByReaderId(userId);

        return new ReaderPublicProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getReaderLevel(),
                user.getStreakDays(),
                readCount,
                favCount,
                commentCount,
                subCount
        );
    }

    @Transactional
    public void recordReadHistory(Long userId, Long postId) {
        ReadHistoryEntity entity = readHistoryRepository.findByUserIdAndPostId(userId, postId)
                .orElseGet(ReadHistoryEntity::new);
        boolean isNew = entity.getId() == null;
        entity.setUserId(userId);
        entity.setPostId(postId);
        entity.setReadAt(LocalDateTime.now());
        if (isNew) {
            entity.setDurationSeconds(0);
            entity.setScrollPosition(0);
        }
        readHistoryRepository.save(entity);
        if (isNew) {
            addExperience(userId, 1);
        }
    }

    @Transactional
    public void updateReadingProgress(Long userId, Long postId, int durationSeconds, int scrollPosition) {
        ReadHistoryEntity entity = readHistoryRepository.findByUserIdAndPostId(userId, postId)
                .orElse(null);
        if (entity != null) {
            entity.setDurationSeconds(Math.max(entity.getDurationSeconds(), durationSeconds));
            entity.setScrollPosition(scrollPosition);
            readHistoryRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public ReadHistoryDetailDTO getReadingProgress(Long userId, Long postId) {
        ReadHistoryEntity entity = readHistoryRepository.findByUserIdAndPostId(userId, postId)
                .orElse(null);
        if (entity == null) return null;
        return toReadHistoryDetailDTO(entity);
    }

    @Transactional
    public void clearReadHistory(Long userId) {
        readHistoryRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void toggleFavorite(Long userId, Long postId) {
        if (favoriteRepository.existsByUserIdAndPostId(userId, postId)) {
            favoriteRepository.deleteByUserIdAndPostId(userId, postId);
        } else {
            FavoriteEntity entity = new FavoriteEntity();
            entity.setUserId(userId);
            entity.setPostId(postId);
            entity.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(entity);
            addExperience(userId, 3);
        }
    }

    @Transactional
    public CommentDTO createComment(Long userId, Long postId, String content) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        int maxLen = getMaxCommentLength(user.getReaderLevel());
        if (content.length() > maxLen) {
            throw new BadRequestException("当前等级评论最长" + maxLen + "字");
        }

        CommentEntity entity = new CommentEntity();
        entity.setUserId(userId);
        entity.setPostId(postId);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        commentRepository.save(entity);

        addExperience(userId, 5);

        return new CommentDTO(
                entity.getId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                postId,
                null,
                content,
                entity.getCreatedAt().toString()
        );
    }

    @Transactional
    public void toggleSubscription(Long readerId, Long authorId) {
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("作者不存在"));
        if (author.getRole() == UserRole.READER) {
            throw new BadRequestException("不能订阅普通读者");
        }

        if (subscriptionRepository.existsByReaderIdAndAuthorId(readerId, authorId)) {
            subscriptionRepository.deleteByReaderIdAndAuthorId(readerId, authorId);
        } else {
            SubscriptionEntity entity = new SubscriptionEntity();
            entity.setReaderId(readerId);
            entity.setAuthorId(authorId);
            entity.setCreatedAt(LocalDateTime.now());
            subscriptionRepository.save(entity);
            addExperience(readerId, 2);
        }
    }

    @Transactional
    public void updateStreakAndExp(Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() != UserRole.READER) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();

        if (lastActive == null || lastActive.isBefore(today.minusDays(1))) {
            user.setStreakDays(1);
        } else if (lastActive.equals(today.minusDays(1))) {
            user.setStreakDays(user.getStreakDays() + 1);
        }

        if (lastActive == null || !lastActive.equals(today)) {
            addExperience(userId, 3);
        }

        user.setLastActiveDate(today);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public SecurityQuestionDTO getSecurityQuestion(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new BadRequestException("该账号未设置安全问题");
        }
        return new SecurityQuestionDTO(user.getUsername(), user.getSecurityQuestion());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        if (user.getSecurityAnswer() == null) {
            throw new BadRequestException("该账号未设置安全问题，无法通过此方式重置密码");
        }

        if (!passwordEncoder.matches(request.securityAnswer(), user.getSecurityAnswer())) {
            throw new BadRequestException("安全答案验证失败");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void addExperience(Long userId, int exp) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() != UserRole.READER) {
            return;
        }
        user.setReaderExp(user.getReaderExp() + exp);
        int newLevel = calculateLevel(user.getReaderExp());
        if (newLevel != user.getReaderLevel()) {
            user.setReaderLevel(newLevel);
        }
        userRepository.save(user);
    }

    int calculateLevel(int exp) {
        int level = 1;
        for (int i = 1; i < LEVEL_THRESHOLDS.length; i++) {
            if (exp >= LEVEL_THRESHOLDS[i]) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    public int getMaxCommentLength(int level) {
        if (level >= 8) return 2000;
        if (level >= 5) return 1000;
        if (level >= 3) return 500;
        return 200;
    }

    public int getCommentIntervalSeconds(int level) {
        if (level >= 6) return 10;
        if (level >= 3) return 30;
        return 60;
    }

    @Transactional(readOnly = true)
    public Page<ReadHistoryDetailDTO> getReadHistory(Long userId, int page, int size) {
        return readHistoryRepository.findByUserIdOrderByReadAtDesc(userId, PageRequest.of(page, size))
                .map(this::toReadHistoryDetailDTO);
    }

    @Transactional(readOnly = true)
    public Page<ReadHistoryDetailDTO> getReadHistoryByCategory(Long userId, Long categoryId, int page, int size) {
        return readHistoryRepository.findByUserIdAndCategoryIdOrderByReadAtDesc(userId, categoryId, PageRequest.of(page, size))
                .map(this::toReadHistoryDetailDTO);
    }

    @Transactional(readOnly = true)
    public Page<FavoriteDetailDTO> getFavorites(Long userId, int page, int size) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toFavoriteDetailDTO);
    }

    @Transactional(readOnly = true)
    public Page<CommentEntity> getComments(Long userId, int page, int size) {
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionEntity> getSubscriptions(Long readerId, int page, int size) {
        return subscriptionRepository.findByReaderIdOrderByCreatedAtDesc(readerId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<CommentEntity> getPostComments(Long postId, int page, int size) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId, PageRequest.of(page, size));
    }

    private UserEntity findReaderOrThrow(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (user.getRole() != UserRole.READER) {
            throw new ForbiddenException("仅读者可使用此功能");
        }
        return user;
    }

    private ReaderProfileDTO buildReaderProfile(UserEntity user) {
        long readCount = readHistoryRepository.countByUserId(user.getId());
        long favCount = favoriteRepository.countByUserId(user.getId());
        long commentCount = commentRepository.countByUserId(user.getId());
        long subCount = subscriptionRepository.countByReaderId(user.getId());

        return new ReaderProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole().name(),
                user.getReaderLevel(),
                user.getReaderExp(),
                user.isShowFootprint(),
                user.getStreakDays(),
                readCount,
                favCount,
                commentCount,
                subCount,
                getMaxCommentLength(user.getReaderLevel()),
                getCommentIntervalSeconds(user.getReaderLevel())
        );
    }

    private ReadHistoryDetailDTO toReadHistoryDetailDTO(ReadHistoryEntity entity) {
        PostEntity post = postRepository.findById(entity.getPostId()).orElse(null);
        boolean postDeleted = post == null;
        String title = post != null ? post.getTitle() : "已删除的作品";
        String excerpt = post != null ? excerpt(post.getContent(), 100) : "该作品已被删除";
        String authorName = post != null && post.getAuthor() != null ? post.getAuthor().getDisplayName() : "";
        return new ReadHistoryDetailDTO(
                entity.getId(),
                entity.getPostId(),
                title,
                excerpt,
                authorName,
                postDeleted,
                entity.getDurationSeconds(),
                entity.getScrollPosition(),
                entity.getReadAt()
        );
    }

    private FavoriteDetailDTO toFavoriteDetailDTO(FavoriteEntity entity) {
        PostEntity post = postRepository.findById(entity.getPostId()).orElse(null);
        boolean postDeleted = post == null;
        String title = post != null ? post.getTitle() : "已删除的作品";
        String excerpt = post != null ? excerpt(post.getContent(), 100) : "该作品已被删除";
        String authorName = post != null && post.getAuthor() != null ? post.getAuthor().getDisplayName() : "";
        return new FavoriteDetailDTO(
                entity.getId(),
                entity.getPostId(),
                title,
                excerpt,
                authorName,
                postDeleted,
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    private String excerpt(String content, int length) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= length) return normalized;
        return normalized.substring(0, length) + "...";
    }
}
