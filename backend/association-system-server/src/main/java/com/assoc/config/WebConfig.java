package com.assoc.config;

import com.assoc.security.JwtInterceptor;
import com.assoc.security.JwtUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：JWT 工具与拦截器注册、跨域（开发环境放开，生产经 Nginx 同域代理）。
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;
    private final com.assoc.mapper.SysUserMapper sysUserMapper;

    public WebConfig(AppProperties properties, com.assoc.mapper.SysUserMapper sysUserMapper) {
        this.properties = properties;
        this.sysUserMapper = sysUserMapper;
    }

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(properties.getJwt().getSecret(), properties.getJwt().getExpireHours());
    }

    @Bean
    public JwtInterceptor jwtInterceptor(JwtUtil jwtUtil) {
        return new JwtInterceptor(jwtUtil, sysUserMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor(jwtUtil()))
                .addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .maxAge(3600);
    }
}
