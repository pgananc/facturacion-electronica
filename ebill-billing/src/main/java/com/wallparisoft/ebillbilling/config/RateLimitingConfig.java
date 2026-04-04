package com.wallparisoft.ebillbilling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Configuración para rate limiting con Redis. */
@Configuration
@EnableAspectJAutoProxy
public class RateLimitingConfig {

  @Bean
  public StringRedisTemplate rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
