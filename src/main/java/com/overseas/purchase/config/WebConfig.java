package com.overseas.purchase.config;

import com.overseas.purchase.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web閰嶇疆绫? */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/legal/current",
                        "/category/list",
                        "/product/list",
                        "/product/detail/**",
                        "/order/estimate",
                        "/security-question/list",
                        "/user/reset-password-by-questions",
                        "/user/reset-password-by-contact",
                        "/upload/**",
                        "/uploads/**",
                        "/payment/notify/wechat",
                        "/static/**",
                        "/seller-review/list"
                );
    }

    /**
     * 鍥剧墖闈欐€佽祫婧愭槧灏?     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path resolvedUploadPath = Paths.get(uploadPath).toAbsolutePath().normalize();
        String resourceLocation = "file:" + resolvedUploadPath + File.separator;

        registry.addResourceHandler("/upload/**")
                .addResourceLocations(resourceLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
