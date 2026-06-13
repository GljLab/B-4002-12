package com.label4002.blog.service;

import com.label4002.blog.entity.CaptchaEntity;
import com.label4002.blog.repository.CaptchaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class CaptchaService {

    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int MAX_CAPTCHA_PER_MINUTE = 10;

    private final CaptchaRepository captchaRepository;

    public CaptchaService(CaptchaRepository captchaRepository) {
        this.captchaRepository = captchaRepository;
    }

    @Transactional
    public CaptchaResult generateCaptcha() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(1);
        long recentCount = captchaRepository.countByCreatedAtAfter(threshold);
        if (recentCount >= MAX_CAPTCHA_PER_MINUTE) {
            throw new com.label4002.blog.exception.BadRequestException("请求过于频繁，请稍后再试");
        }

        LocalDateTime expiry = now.minusMinutes(CAPTCHA_EXPIRE_MINUTES);
        captchaRepository.deleteByCreatedAtBefore(expiry);

        String code = generateRandomCode(CAPTCHA_LENGTH);
        String key = UUID.randomUUID().toString().replace("-", "");
        String svg = buildSvgCaptcha(code);

        CaptchaEntity entity = new CaptchaEntity();
        entity.setId(key);
        entity.setCode(code);
        entity.setCreatedAt(now);
        entity.setUsed(false);
        captchaRepository.save(entity);

        return new CaptchaResult(key, "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)));
    }

    @Transactional
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        return captchaRepository.findById(captchaKey)
                .filter(entity -> !entity.isUsed())
                .filter(entity -> entity.getCreatedAt().plusMinutes(CAPTCHA_EXPIRE_MINUTES).isAfter(LocalDateTime.now()))
                .filter(entity -> entity.getCode().equalsIgnoreCase(captchaCode.trim()))
                .map(entity -> {
                    entity.setUsed(true);
                    captchaRepository.save(entity);
                    return true;
                })
                .orElse(false);
    }

    public String hashIp(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String buildSvgCaptcha(String code) {
        int width = 120;
        int height = 40;
        SecureRandom random = new SecureRandom();

        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\">", width, height));
        svg.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"#f0f4f8\" rx=\"6\"/>", width, height));

        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            String color = String.format("#%06x", random.nextInt(0xCCCCCC));
            svg.append(String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\" stroke-width=\"1\"/>", x1, y1, x2, y2, color));
        }

        int charWidth = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            int x = charWidth * (i + 1) - 4;
            int y = 26 + random.nextInt(8) - 4;
            int rotation = random.nextInt(30) - 15;
            String color = String.format("#%02x%02x%02x", 30 + random.nextInt(80), 30 + random.nextInt(80), 100 + random.nextInt(155));
            svg.append(String.format("<text x=\"%d\" y=\"%d\" font-family=\"Arial,sans-serif\" font-size=\"22\" font-weight=\"bold\" fill=\"%s\" transform=\"rotate(%d,%d,%d)\">%s</text>",
                    x, y, color, rotation, x, y, code.charAt(i)));
        }

        for (int i = 0; i < 30; i++) {
            int cx = random.nextInt(width);
            int cy = random.nextInt(height);
            String color = String.format("#%06x", random.nextInt(0xCCCCCC));
            svg.append(String.format("<circle cx=\"%d\" cy=\"%d\" r=\"1\" fill=\"%s\"/>", cx, cy, color));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    public record CaptchaResult(String key, String svgDataUrl) {
    }
}
