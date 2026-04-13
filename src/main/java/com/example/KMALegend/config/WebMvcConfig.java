package com.example.KMALegend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.KMALegend.encode.EncryptionInterceptor;

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
                .excludePathPatterns(
                        "/api/v1/encryption/public-key",
                        "/api/v1/score-batch/encrypted/student"
                );
    }
}
