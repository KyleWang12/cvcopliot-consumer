package com.cvcopilot.resumebuilding.service;

import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class StateService {

  private RedisTemplate<String, String> redisTemplate;

  private HashOperations<String, String, String> hashOperations;
  private ZSetOperations<String, String> zSetOperations;

  @Autowired
  public StateService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  private void init() {
    hashOperations = redisTemplate.opsForHash();
    zSetOperations = redisTemplate.opsForZSet();
  }

  public void addOrUpdateState(String userId, String modificationId, String state) {
    String modificationKey = "state:modification:" + modificationId;
    long timestamp = System.currentTimeMillis();
    hashOperations.put(modificationKey, "state", state);
    hashOperations.put(modificationKey, "userId", userId);
    hashOperations.put(modificationKey, "lastUpdate", String.valueOf(timestamp));
  }
}
