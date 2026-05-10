package com.vishnusinha.orchestrator.scheduler.job;

import com.vishnusinha.orchestrator.scheduler.config.QueueProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RedisJobQueue implements JobQueue {

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties queueProperties;

    public RedisJobQueue(StringRedisTemplate redisTemplate, QueueProperties queueProperties) {
        this.redisTemplate = redisTemplate;
        this.queueProperties = queueProperties;
    }

    @Override
    public void enqueue(UUID jobId) {
        Long queueDepth = redisTemplate.opsForList().rightPush(queueProperties.pendingJobsKey(), jobId.toString());
        if (queueDepth == null) {
            throw new QueueUnavailableException("Redis did not acknowledge job enqueue");
        }
    }

    @Override
    public Optional<UUID> dequeue() {
        String value = redisTemplate.opsForList().leftPop(queueProperties.pendingJobsKey());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public void requeue(UUID jobId) {
        Long queueDepth = redisTemplate.opsForList().leftPush(queueProperties.pendingJobsKey(), jobId.toString());
        if (queueDepth == null) {
            throw new QueueUnavailableException("Redis did not acknowledge job requeue");
        }
    }
}
