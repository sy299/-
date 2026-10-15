package com.community.mall.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
  public void addCorsMappings(CorsRegistry r) { r.addMapping("/**").allowedOrigins("http://localhost:5173","http://127.0.0.1:5173").allowedMethods("*").allowedHeaders("*"); }
}
