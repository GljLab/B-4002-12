package com.label4002.blog.security;

import com.label4002.blog.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;
    private final boolean enabled;

    public AppUserPrincipal(Long id, String username, String password, List<GrantedAuthority> authorities, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public static AppUserPrincipal fromDatabase(Long id, String username, String password, UserRole role, boolean enabled) {
        return new AppUserPrincipal(
                id,
                username,
                password,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                enabled
        );
    }

    public static AppUserPrincipal fromJwt(Long id, String username) {
        return new AppUserPrincipal(
                id,
                username,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_TOKEN")),
                true
        );
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
