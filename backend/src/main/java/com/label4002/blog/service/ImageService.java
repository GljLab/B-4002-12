package com.label4002.blog.service;

import com.label4002.blog.dto.ImageDTO;
import com.label4002.blog.dto.PostSummaryDTO;
import com.label4002.blog.entity.ImageEntity;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.exception.BadRequestException;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.repository.ImageRepository;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Pattern IMAGE_REF_PATTERN = Pattern.compile("!\\[.*?\\]\\((.*?/uploads/.*?)\\)");

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public ImageService(ImageRepository imageRepository,
                        UserRepository userRepository,
                        PostRepository postRepository) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public ImageDTO uploadImage(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("上传文件不能为空");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("文件大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("仅支持JPEG、PNG、GIF、WebP格式的图片");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + extension;

        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BadRequestException("文件上传失败: " + e.getMessage());
        }

        ImageEntity image = new ImageEntity();
        image.setFilename(filename);
        image.setOriginalName(originalFilename);
        image.setAuthor(user);
        image.setContentType(contentType);
        image.setFileSize(file.getSize());
        image = imageRepository.save(image);

        return toDTO(image);
    }

    @Transactional(readOnly = true)
    public List<ImageDTO> listByAuthor(Long userId) {
        return imageRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImageDTO getImage(Long imageId) {
        ImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
        return toDTOWithReferences(image);
    }

    @Transactional
    public ImageDTO renameImage(Long userId, Long imageId, String newOriginalName) {
        ImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("图片不存在"));

        if (!image.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("无权限操作他人的图片");
        }

        image.setOriginalName(newOriginalName);
        image = imageRepository.save(image);
        return toDTO(image);
    }

    @Transactional
    public void deleteImage(Long userId, Long imageId) {
        ImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("图片不存在"));

        if (!image.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("无权限操作他人的图片");
        }

        List<PostEntity> referencingPosts = findReferencingPosts(image.getFilename());
        if (!referencingPosts.isEmpty()) {
            throw new BadRequestException("该图片被 " + referencingPosts.size() + " 篇文章引用，请先移除引用后再删除");
        }

        try {
            Path filePath = Paths.get(uploadPath).resolve(image.getFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BadRequestException("文件删除失败");
        }

        imageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDTO> getReferencingPosts(Long imageId) {
        ImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("图片不存在"));
        List<PostEntity> posts = findReferencingPosts(image.getFilename());
        return posts.stream().map(this::toPostSummary).toList();
    }

    public Path resolveImagePath(String filename) {
        Path filePath = Paths.get(uploadPath).resolve(filename);
        if (!filePath.normalize().startsWith(Paths.get(uploadPath).normalize())) {
            throw new BadRequestException("非法文件路径");
        }
        if (!Files.exists(filePath)) {
            throw new NotFoundException("图片不存在");
        }
        return filePath;
    }

    private List<PostEntity> findReferencingPosts(String filename) {
        String imageUrl = "/uploads/" + filename;
        List<PostEntity> allPosts = postRepository.findAll();
        List<PostEntity> referencing = new ArrayList<>();
        for (PostEntity post : allPosts) {
            if (post.getContent() != null && post.getContent().contains(imageUrl)) {
                referencing.add(post);
            }
        }
        return referencing;
    }

    private ImageDTO toDTO(ImageEntity image) {
        return new ImageDTO(
                image.getId(),
                image.getFilename(),
                image.getOriginalName(),
                baseUrl + "/uploads/" + image.getFilename(),
                image.getAuthor().getId(),
                image.getContentType(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedAt(),
                null
        );
    }

    private ImageDTO toDTOWithReferences(ImageEntity image) {
        List<PostEntity> posts = findReferencingPosts(image.getFilename());
        List<PostSummaryDTO> postDTOs = posts.stream().map(this::toPostSummary).toList();
        return new ImageDTO(
                image.getId(),
                image.getFilename(),
                image.getOriginalName(),
                baseUrl + "/uploads/" + image.getFilename(),
                image.getAuthor().getId(),
                image.getContentType(),
                image.getFileSize(),
                image.getWidth(),
                image.getHeight(),
                image.getCreatedAt(),
                postDTOs
        );
    }

    private PostSummaryDTO toPostSummary(PostEntity post) {
        String excerpt = post.getContent() != null && post.getContent().length() > 100
                ? post.getContent().trim().replaceAll("\\s+", " ").substring(0, 100) + "..."
                : (post.getContent() != null ? post.getContent().trim().replaceAll("\\s+", " ") : "");
        return new PostSummaryDTO(
                post.getId(),
                post.getTitle(),
                excerpt,
                excerpt, // previewContent
                post.getAuthor().getId(),
                post.getAuthor().getDisplayName(),
                post.getAuthor().getAvatarUrl(),
                post.getStatus().name(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                "",
                List.of(),
                post.getViewCount(),
                0L, // favoriteCount
                post.getScheduledAt(),
                post.isRevision(),
                post.getParentPost() != null ? post.getParentPost().getId() : null,
                false,
                post.getCreatedAt()
        );
    }
}
