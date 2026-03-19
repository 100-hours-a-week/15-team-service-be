package com.sipomeokjo.commitme.domain.resume.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * CLI runner for projection rebuild.
 *
 * <p>Usage:
 *
 * <ul>
 *   <li>{@code --rebuild-projection=all} — rebuild all resumes
 *   <li>{@code --rebuild-projection=123} — rebuild single resume by ID
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeProjectionRebuildCommand implements ApplicationRunner {

    private final ResumeProjectionRebuildService resumeProjectionRebuildService;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("rebuild-projection")) {
            return;
        }

        List<String> values = args.getOptionValues("rebuild-projection");
        if (values == null || values.isEmpty()) {
            log.warn("[REBUILD_CMD] no value provided for --rebuild-projection");
            return;
        }

        String value = values.get(0).trim();
        log.info("[REBUILD_CMD] starting rebuild-projection={}", value);

        if ("all".equalsIgnoreCase(value)) {
            resumeProjectionRebuildService.rebuildAll();
        } else {
            try {
                Long resumeId = Long.parseLong(value);
                resumeProjectionRebuildService.rebuildForResume(resumeId);
            } catch (NumberFormatException e) {
                log.error("[REBUILD_CMD] invalid resumeId '{}' — use 'all' or a numeric ID", value);
            }
        }
    }
}
