package com.example.KMALegend.config;

import com.example.KMALegend.encode.EncryptionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final EncryptionInterceptor encryptionInterceptor;

    public WebMvcConfig(EncryptionInterceptor encryptionInterceptor) {
        this.encryptionInterceptor = encryptionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(encryptionInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/encryption/public-key");
    }
} 