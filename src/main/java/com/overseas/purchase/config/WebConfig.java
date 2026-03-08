package com.overseas.purchase.config;

import com.overseas.purchase.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

/**
 * Web配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/category/list",
                        "/product/list",
                        "/product/detail/**",
                        "/security-question/list",
                        "/user/reset-password-by-questions",
                        "/user/reset-password-by-contact",
                        "/upload/**",
                        "/uploads/**",
                        "/static/**",
                        "/seller-review/list"
                );
    }

    /**
     * 图片静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用绝对路径映射
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath);
                
        // 兼容旧配置
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}