package com.label4002.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long accessMinutes;
    private long refreshDays;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessMinutes() {
        return accessMinutes;
    }

    public void setAccessMinutes(long accessMinutes) {
        this.accessMinutes = accessMinutes;
    }

    public long getRefreshDays() {
        return refreshDays;
    }

    public void setRefreshDays(long refreshDays) {
        this.refreshDays = refreshDays;
    }
}
