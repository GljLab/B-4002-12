package com.label4002.blog.service;

import com.label4002.blog.dto.AuthUserDTO;
import com.label4002.blog.dto.LoginRequest;
import com.label4002.blog.dto.TokenRefreshRequest;
import com.label4002.blog.dto.TokenResponse;
import com.label4002.blog.entity.RefreshTokenEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.entity.UserRole;
import com.label4002.blog.exception.NotFoundException;
import com.label4002.blog.exception.UnauthorizedException;
import com.label4002.blog.repository.RefreshTokenRepository;
import com.label4002.blog.repository.UserRepository;
import com.label4002.blog.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthUserDTO loginSession(LoginRequest request,
                                    HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("账号已被禁用");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("账号已锁定，请" + LOCK_MINUTES + "分钟后再试");
        }

        Authentication authentication;
        try {
            authentication = authenticate(request);
            user.setLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (UnauthorizedException ex) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            userRepository.save(user);
            throw ex;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        httpServletRequest.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        UserEntity refreshedUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        Integer readerLevel = refreshedUser.getRole() == UserRole.READER ? refreshedUser.getReaderLevel() : null;
        return new AuthUserDTO(
                refreshedUser.getId(),
                refreshedUser.getUsername(),
                refreshedUser.getRole().name(),
                refreshedUser.getDisplayName(),
                refreshedUser.getAvatarUrl(),
                readerLevel
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    @Transactional(readOnly = true)
    public AuthUserDTO me(AppUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("未登录");
        }
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        Integer readerLevel = user.getRole() == UserRole.READER ? user.getReaderLevel() : null;
        return new AuthUserDTO(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                readerLevel
        );
    }

    @Transactional
    public TokenResponse loginToken(LoginRequest request) {
        Authentication authentication = authenticate(request);
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        String tokenHash = jwtService.sha256(request.refreshToken());
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("refreshToken无效"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new UnauthorizedException("refreshToken已过期");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        return issueTokenPair(refreshToken.getUser());
    }

    private Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.username(),
                    request.password()
            ));
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("用户名或密码错误");
        } catch (LockedException ex) {
            throw new UnauthorizedException("账号已锁定");
        }
    }

    private TokenResponse issueTokenPair(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshTokenRaw = jwtService.generateRefreshTokenRaw();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(jwtService.sha256(refreshTokenRaw));
        refreshToken.setExpiresAt(jwtService.refreshTokenExpiresAt());
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                "Bearer",
                accessToken,
                jwtService.accessTokenExpiresInSeconds(),
                refreshTokenRaw
        );
    }
}
