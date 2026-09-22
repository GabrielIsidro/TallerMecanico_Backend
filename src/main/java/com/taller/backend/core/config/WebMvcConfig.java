package com.taller.backend.core.config;

import com.taller.backend.core.security.SuscripcionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SuscripcionInterceptor suscripcionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(suscripcionInterceptor)
                .addPathPatterns("/api/v1/talleres/**")
                .excludePathPatterns(
                        "/api/v1/talleres/auth/**",
                        "/api/v1/talleres/usuarios/me"
                );
    }
}
