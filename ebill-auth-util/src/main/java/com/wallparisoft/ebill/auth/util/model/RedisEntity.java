package com.wallparisoft.ebill.auth.util.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults (level = AccessLevel.PRIVATE)
@Builder
public class RedisEntity {

    String key;
    String value;
    int duration;
}

