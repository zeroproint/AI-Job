package com.aijob.aijobbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：供前端 H5（本地 Vite/HBuilderX 或直接打开的 dist）访问后端接口。
 * <p>
 * 当前为开发演示阶段，放开所有来源；上线前应改为具体前端域名。
 * 未开启 allowCredentials，因此不使用 Cookie 等凭证。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
