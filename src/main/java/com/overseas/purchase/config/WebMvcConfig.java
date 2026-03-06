package com.overseas.purchase.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 
 * @author System
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用 Controller 手动处理图片请求，不再使用静态资源映射
        // 这样可以避免路径解析问题，也可以更灵活地控制权限和缓存
        
        // 配置收款码图片访问路径
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 配置上传文件访问路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:D:/upload/overseas-purchase/");
    }
}
