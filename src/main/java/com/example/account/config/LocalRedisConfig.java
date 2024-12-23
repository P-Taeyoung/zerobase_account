package com.example.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Configuration
public class LocalRedisConfig {
    @Value("${spring.redis.port}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        String architecture = System.getProperty("os.arch");
        if ("aarch64".equals(architecture)) {
            // ARM64 환경에서 사용할 Redis 바이너리 경로를 지정합니다.
            File redisExecutable = new File("src/main/resources/redis/redis-server-mac-arm64");
            if (!redisExecutable.exists()) {
                throw new IllegalStateException("ARM64용 Redis 바이너리가 경로에 없습니다: " + redisExecutable.getAbsolutePath());
            }
            redisServer = new RedisServer(redisExecutable, redisPort);
        } else {
            redisServer = new RedisServer(redisPort); // x86_64는 기본 내장 바이너리 사용
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
