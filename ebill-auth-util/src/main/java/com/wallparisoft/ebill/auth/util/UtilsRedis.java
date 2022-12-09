package com.wallparisoft.ebill.auth.util;


import com.wallparisoft.ebill.auth.util.model.RedisEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UtilsRedis {
    final StringRedisTemplate stringRedisTemplate;

   public UtilsRedis(StringRedisTemplate stringRedisTemplate) {
       this.stringRedisTemplate = stringRedisTemplate;
   }

    public String registerKey(RedisEntity redis) {
        ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
        objectRedis.set(redis.getKey(), redis.getValue(), Duration.ofMinutes(redis.getDuration()));

        return redis.getKey();
    }

    public boolean checkExistRegister(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    public String getValue(String key) {
        ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
        return objectRedis.get(key);
    }

    public boolean removeKey(String key) {
        return stringRedisTemplate.delete(key);
    }
}
