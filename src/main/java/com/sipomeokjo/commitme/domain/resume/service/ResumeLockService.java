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
    private static final String EDIT_KEY_PREFIX = "resume:lock:edit:";

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private static final DefaultRedisScript<Long> BIND_OWNER_SCRIPT;
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT;

    public enum LockAcquireResult {
        ACQUIRED,
        BUSY,
        FALLBACK
    }

    static {
        BIND_OWNER_SCRIPT = new DefaultRedisScript<>();
        BIND_OWNER_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "  local ttl = redis.call('pttl', KEYS[1]) "
                        + "  if ttl > 0 then "
                        + "    redis.call('psetex', KEYS[1], ttl, ARGV[2]) "
                        + "  else "
                        + "    redis.call('set', KEYS[1], ARGV[2]) "
                        + "  end "
                        + "  return 1 "
                        + "end "
                        + "return 0");
        BIND_OWNER_SCRIPT.setResultType(Long.class);

        RELEASE_SCRIPT = new DefaultRedisScript<>();
        RELEASE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "  return redis.call('del', KEYS[1]) "
                        + "end "
                        + "return 0");
        RELEASE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public LockAcquireResult tryAcquireCreateLock(Long userId, String ownerToken) {
        return tryAcquireLock(CREATE_KEY_PREFIX + userId, ownerToken, "create", "userId", userId);
    }

    public boolean bindCreateLockOwner(Long userId, String expectedToken, Long resumeId) {
        return bindOwner(CREATE_KEY_PREFIX + userId, expectedToken, String.valueOf(resumeId));
    }

    public void releaseCreateLock(Long userId, Long resumeId) {
        releaseCreateLockByToken(userId, String.valueOf(resumeId));
    }

    public void releaseCreateLockByToken(Long userId, String ownerToken) {
        releaseLock(CREATE_KEY_PREFIX + userId, ownerToken, "create", "userId", userId);
    }

    public LockAcquireResult tryAcquireEditLock(Long resumeId, String ownerToken) {
        return tryAcquireLock(EDIT_KEY_PREFIX + resumeId, ownerToken, "edit", "resumeId", resumeId);
    }

    public boolean bindEditLockOwner(Long resumeId, String expectedToken, Integer versionNo) {
        return bindOwner(
                EDIT_KEY_PREFIX + resumeId, expectedToken, buildEditLockToken(resumeId, versionNo));
    }

    public void releaseEditLock(Long resumeId, Integer versionNo) {
        releaseEditLockByToken(resumeId, buildEditLockToken(resumeId, versionNo));
    }

    public void releaseEditLockByToken(Long resumeId, String ownerToken) {
        releaseLock(EDIT_KEY_PREFIX + resumeId, ownerToken, "edit", "resumeId", resumeId);
    }

    private LockAcquireResult tryAcquireLock(
            String lockKey,
            String ownerToken,
            String kind,
            String subjectName,
            Object subjectValue) {
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
                    "[RESUME_LOCK] {}_acquire_error {}={} — fall-through to DB",
                    kind,
                    subjectName,
                    subjectValue,
                    e);
            meterRegistry.counter("resume.lock.fallback.total", "kind", kind).increment();
            return LockAcquireResult.FALLBACK;
        }
    }

    public String buildEditLockToken(Long resumeId, Integer versionNo) {
        return resumeId + ":" + versionNo;
    }

    private boolean bindOwner(String lockKey, String expectedToken, String boundOwnerToken) {
        try {
            Long bound =
                    stringRedisTemplate.execute(
                            BIND_OWNER_SCRIPT, List.of(lockKey), expectedToken, boundOwnerToken);
            return Long.valueOf(1L).equals(bound);
        } catch (Exception e) {
            log.warn("[RESUME_LOCK] owner_bind_error key={}", lockKey, e);
            return false;
        }
    }

    private void releaseLock(
            String lockKey,
            String ownerToken,
            String kind,
            String subjectName,
            Object subjectValue) {
        try {
            Long released =
                    stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(lockKey), ownerToken);
            if (!Long.valueOf(1L).equals(released)) {
                meterRegistry.counter("resume.lock.release.meta_missing.total").increment();
                log.warn(
                        "[RESUME_LOCK] {}_release_token_mismatch {}={} owner={} — 이미 만료 또는 미존재",
                        kind,
                        subjectName,
                        subjectValue,
                        ownerToken);
            }
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] {}_release_error {}={} — TTL 만료 대기",
                    kind,
                    subjectName,
                    subjectValue,
                    e);
        }
    }
}
