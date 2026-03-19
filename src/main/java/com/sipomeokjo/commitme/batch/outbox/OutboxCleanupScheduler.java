package com.sipomeokjo.commitme.batch.outbox;

import com.sipomeokjo.commitme.config.OutboxCleanupProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final JobLauncher syncJobLauncher;
    private final Job outboxCleanupJob;
    private final OutboxCleanupProperties props;

    public OutboxCleanupScheduler(
            @Qualifier("syncJobLauncher") JobLauncher syncJobLauncher,
            Job outboxCleanupJob,
            OutboxCleanupProperties props) {
        this.syncJobLauncher = syncJobLauncher;
        this.outboxCleanupJob = outboxCleanupJob;
        this.props = props;
    }

    @Scheduled(cron = "${app.batch.outbox-cleanup.cron}")
    @SchedulerLock(
            name = "outboxCleanupJob",
            lockAtMostFor = "${app.batch.outbox-cleanup.lock-at-most}",
            lockAtLeastFor = "${app.batch.outbox-cleanup.lock-at-least}")
    public void run() {
        if (!props.enabled()) {
            log.debug("Outbox cleanup batch is disabled, skipping.");
            return;
        }

        try {
            Instant cutoff = Instant.now().minus(props.retentionDays(), ChronoUnit.DAYS);
            JobParameters params =
                    new JobParametersBuilder()
                            .addString("cutoffInstant", cutoff.toString())
                            .toJobParameters();
            log.info("Starting outbox cleanup job with cutoff={}", cutoff);
            syncJobLauncher.run(outboxCleanupJob, params);
        } catch (Exception e) {
            log.error("Outbox cleanup job failed", e);
        }
    }
}
