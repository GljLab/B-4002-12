package com.label4002.blog.config;

import com.label4002.blog.security.CustomUserDetailsService;
import com.label4002.blog.security.JwtAuthenticationFilter;
import com.label4002.blog.security.RestAccessDeniedHandler;
import com.label4002.blog.security.RestAuthenticationEntryPoint;
import com.label4002.blog.security.TraceIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${app.testing.enabled:false}")
    private boolean testingEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   TraceIdFilter traceIdFilter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler deniedHandler,
                                                   DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/authors", "/api/v1/authors/*", "/api/v1/authors/*/posts").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/*", "/api/v1/categories/flat").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/keywords/cloud").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/keywords/search").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/images/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/upload/image").authenticated();
                    auth.requestMatchers("/api/v1/auth/login", "/api/v1/token/login", "/api/v1/token/refresh").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/reader/captcha").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/reader/register").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/reader/security-question").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/reader/reset-password").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/reader/*").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/collections/public/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/collections/ranking").permitAll();
                    auth.requestMatchers("/api/v1/collections/**").authenticated();
                    if (testingEnabled) {
                        auth.requestMatchers("/api/testing/**").permitAll();
                    }
                    auth.requestMatchers("/api/v1/token/secure/**").hasRole("TOKEN");
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/author/**").hasRole("AUTHOR");
                    auth.requestMatchers("/api/v1/reader/**").hasAnyRole("READER", "ADMIN");
                    auth.requestMatchers("/api/v1/auth/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
