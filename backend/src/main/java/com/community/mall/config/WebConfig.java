package com.community.mall.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;
@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final Path uploadRoot;
  public WebConfig(@Value("${mall.upload-dir:./uploads}") String uploadDir) {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
  }
  public void addCorsMappings(CorsRegistry r) {
    r.addMapping("/**")
        .allowedOrigins("http://localhost:5173","http://127.0.0.1:5173","http://localhost:5174","http://127.0.0.1:5174")
        .allowedMethods("*").allowedHeaders("*");
  }
  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**").addResourceLocations(uploadRoot.toUri().toString());
  }
}
