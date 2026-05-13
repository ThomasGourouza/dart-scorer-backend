package com.dartscorer.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:4200}")
  private String allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = StringUtils.commaDelimitedListToStringArray(allowedOrigins);
    registry.addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*");
    registry.addMapping("/actuator/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*");
  }
}
