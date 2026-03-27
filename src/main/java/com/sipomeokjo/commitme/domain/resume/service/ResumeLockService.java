package com.sipomeokjo.commitme.domain.resume.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeLockService {

    private static final String CREATE_KEY_PREFIX = "resume:lock:create:";
    private static final String EDIT_KEY_PREFIX = "resume:lock:edit:";
    private static final String EDIT_META_KEY_PREFIX = "resume:lock:edit:meta:";

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration META_TTL = LOCK_TTL.plusSeconds(60);

    /** Lua Script — 락 보유자만 삭제 가능 (원자적 검증 + 삭제). TTL 만료 후 새 락이 생성된 경우 이전 보유자의 해제 시도를 무시한다. */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT;

    static {
        RELEASE_SCRIPT = new DefaultRedisScript<>();
        RELEASE_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "  return redis.call('del', KEYS[1]) "
                        + "else "
                        + "  return 0 "
                        + "end");
        RELEASE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * CREATE 락 획득.
     *
     * <p>lockValue = resumeId — 콜백이 resumeId를 알고 있으므로 별도 저장 불필요.
     *
     * @return true: 락 획득 성공 / false: 이미 선점됨 (→ 즉시 409) Redis 장애 시 true 반환 → MongoDB 체크로 위임
     */
    public boolean tryAcquireCreateLock(Long userId, Long resumeId) {
        String key = CREATE_KEY_PREFIX + userId;
        String value = String.valueOf(resumeId);
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, value, LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] create_acquire_error userId={} — fall-through to DB", userId, e);
            return true;
        }
    }

    /**
     * CREATE 락 해제 (Lua Script).
     *
     * <p>resumeId 값이 일치하는 경우에만 삭제 → TTL 만료 후 새 생성 요청의 락을 삭제하지 않는다.
     */
    public void releaseCreateLock(Long userId, Long resumeId) {
        String key = CREATE_KEY_PREFIX + userId;
        String value = String.valueOf(resumeId);
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(key), value);
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] create_release_error userId={} resumeId={} — TTL 만료 대기",
                    userId,
                    resumeId,
                    e);
        }
    }

    /**
     * EDIT 락 획득.
     *
     * <p>lockValue = UUID — 콜백이 UUID를 알 수 없으므로 메타 키에 별도 저장.
     *
     * @return true: 락 획득 성공 / false: 이미 선점됨 (→ 즉시 409) Redis 장애 시 true 반환 → MongoDB findAndModify로
     *     위임
     */
    public boolean tryAcquireEditLock(Long resumeId) {
        String key = EDIT_KEY_PREFIX + resumeId;
        String metaKey = EDIT_META_KEY_PREFIX + resumeId;
        String uuid = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, uuid, LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                return false;
            }
            stringRedisTemplate.opsForValue().set(metaKey, uuid, META_TTL);
            return true;
        } catch (Exception e) {
            log.warn(
                    "[RESUME_LOCK] edit_acquire_error resumeId={} — fall-through to DB",
                    resumeId,
                    e);
            return true;
        }
    }

    /**
     * EDIT 락 해제 (메타 키로 UUID 조회 → Lua Script).
     *
     * <p>메타 키가 없으면 이미 TTL 만료된 것으로 간주하고 종료.
     */
    public void releaseEditLock(Long resumeId) {
        String key = EDIT_KEY_PREFIX + resumeId;
        String metaKey = EDIT_META_KEY_PREFIX + resumeId;
        try {
            String uuid = stringRedisTemplate.opsForValue().get(metaKey);
            if (uuid == null) {
                log.warn(
                        "[RESUME_LOCK] edit_release_meta_missing resumeId={} — 이미 만료 또는 미존재",
                        resumeId);
                return;
            }
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(key), uuid);
            stringRedisTemplate.delete(metaKey);
        } catch (Exception e) {
            log.warn("[RESUME_LOCK] edit_release_error resumeId={} — TTL 만료 대기", resumeId, e);
        }
    }
}
