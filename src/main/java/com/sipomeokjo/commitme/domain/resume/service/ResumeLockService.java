package com.sipomeokjo.commitme.domain.resume.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeLockService {

    private static final String CREATE_KEY_PREFIX = "resume:lock:create:";
    private static final String CREATE_TIME_KEY_PREFIX = "resume:lock:create:time:";
    private static final String EDIT_KEY_PREFIX = "resume:lock:edit:";

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration META_TTL = LOCK_TTL.plusSeconds(60);

    private static final DefaultRedisScript<Long> CREATE_ACQUIRE_SCRIPT;
    private static final DefaultRedisScript<String> CREATE_RELEASE_SCRIPT;
    private static final DefaultRedisScript<Long> EDIT_RELEASE_SCRIPT;

    public enum LockAcquireResult {
        ACQUIRED,
        BUSY,
        FALLBACK
    }

    static {
        CREATE_ACQUIRE_SCRIPT = new DefaultRedisScript<>();
        CREATE_ACQUIRE_SCRIPT.setScriptText(
                "if redis.call('exists', KEYS[1]) == 1 then "
                        + "  return 0 "
                        + "end "
                        + "redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2]) "
                        + "redis.call('set', KEYS[2], ARGV[3], 'PX', ARGV[4]) "
                        + "return 1");
        CREATE_ACQUIRE_SCRIPT.setResultType(Long.class);

        CREATE_RELEASE_SCRIPT = new DefaultRedisScript<>();
        CREATE_RELEASE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "  local acquiredAt = redis.call('get', KEYS[2]) "
                        + "  redis.call('del', KEYS[1]) "
                        + "  redis.call('del', KEYS[2]) "
                        + "  if acquiredAt then "
                        + "    return acquiredAt "
                        + "  end "
                        + "  return 'RELEASED' "
                        + "end "
                        + "return 'MISMATCH'");
        CREATE_RELEASE_SCRIPT.setResultType(String.class);

        EDIT_RELEASE_SCRIPT = new DefaultRedisScript<>();
        EDIT_RELEASE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "  return redis.call('del', KEYS[1]) "
                        + "end "
                        + "return 0");
        EDIT_RELEASE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public LockAcquireResult tryAcquireCreateLock(Long userId, Long resumeId) {
        String lockKey = CREATE_KEY_PREFIX + userId;
        String timeKey = CREATE_TIME_KEY_PREFIX + userId;
        try {
            Long scriptResult =
                    stringRedisTemplate.execute(
                            CREATE_ACQUIRE_SCRIPT,
                            List.of(lockKey, timeKey),
                            String.valueOf(resumeId),
                            String.valueOf(LOCK_TTL.toMillis()),
                            String.valueOf(System.currentTimeMillis()),
                            String.valueOf(META_TTL.toMillis()));
            return toAcquireResult(scriptResult);
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] create_acquire_error userId={} — fall-through to DB", userId, e);
            meterRegistry.counter("resume.lock.fallback.total", "kind", "create").increment();
            return LockAcquireResult.FALLBACK;
        }
    }

    public void releaseCreateLock(Long userId, Long resumeId) {
        String lockKey = CREATE_KEY_PREFIX + userId;
        String timeKey = CREATE_TIME_KEY_PREFIX + userId;
        try {
            stringRedisTemplate.execute(
                    CREATE_RELEASE_SCRIPT, List.of(lockKey, timeKey), String.valueOf(resumeId));
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] create_release_error userId={} resumeId={} — TTL 만료 대기",
                    userId,
                    resumeId,
                    e);
        }
    }

    public LockAcquireResult tryAcquireEditLock(Long resumeId, Integer versionNo) {
        String lockKey = EDIT_KEY_PREFIX + resumeId;
        String ownerToken = buildEditLockToken(resumeId, versionNo);
        try {
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            Boolean acquired = valueOperations.setIfAbsent(lockKey, ownerToken, LOCK_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                return LockAcquireResult.ACQUIRED;
            }
            if (Boolean.FALSE.equals(acquired)) {
                return LockAcquireResult.BUSY;
            }
            return LockAcquireResult.FALLBACK;
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] edit_acquire_error resumeId={} — fall-through to DB",
                    resumeId,
                    e);
            meterRegistry.counter("resume.lock.fallback.total", "kind", "edit").increment();
            return LockAcquireResult.FALLBACK;
        }
    }

    public void releaseEditLock(Long resumeId, Integer versionNo) {
        String lockKey = EDIT_KEY_PREFIX + resumeId;
        String ownerToken = buildEditLockToken(resumeId, versionNo);
        try {
            Long released =
                    stringRedisTemplate.execute(EDIT_RELEASE_SCRIPT, List.of(lockKey), ownerToken);
            if (!Long.valueOf(1L).equals(released)) {
                meterRegistry.counter("resume.lock.release.meta_missing.total").increment();
                log.warn(
                        "[RESUME_LOCK] edit_release_token_mismatch resumeId={} versionNo={} — 이미 만료 또는 미존재",
                        resumeId,
                        versionNo);
            }
        } catch (Exception e) {
            log.warn("[RESUME_LOCK] edit_release_error resumeId={} — TTL 만료 대기", resumeId, e);
        }
    }

    public String buildEditLockToken(Long resumeId, Integer versionNo) {
        return resumeId + ":" + versionNo;
    }

    private LockAcquireResult toAcquireResult(Long scriptResult) {
        if (Long.valueOf(1L).equals(scriptResult)) {
            return LockAcquireResult.ACQUIRED;
        }
        if (Long.valueOf(0L).equals(scriptResult)) {
            return LockAcquireResult.BUSY;
        }
        return LockAcquireResult.FALLBACK;
    }
}
