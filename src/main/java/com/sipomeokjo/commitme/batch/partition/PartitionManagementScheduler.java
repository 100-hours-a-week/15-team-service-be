package com.sipomeokjo.commitme.batch.partition;

import com.sipomeokjo.commitme.config.PartitionManagementProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PartitionManagementScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartitionManagementScheduler.class);

    private final PartitionManagementService partitionManagementService;
    private final PartitionManagementProperties props;

    public PartitionManagementScheduler(
            PartitionManagementService partitionManagementService,
            PartitionManagementProperties props) {
        this.partitionManagementService = partitionManagementService;
        this.props = props;
    }

    @Scheduled(cron = "${app.batch.partition-management.cron}")
    @SchedulerLock(
            name = "partitionManagementJob",
            lockAtMostFor = "${app.batch.partition-management.lock-at-most}",
            lockAtLeastFor = "${app.batch.partition-management.lock-at-least}")
    public void run() {
        if (!props.enabled()) {
            log.debug("Partition management is disabled, skipping.");
            return;
        }

        try {
            log.info(
                    "Starting partition rotation: monthsAhead={}, monthsToKeep={}",
                    props.monthsAhead(),
                    props.monthsToKeep());
            partitionManagementService.rotate(props.monthsAhead(), props.monthsToKeep());
            log.info("Partition rotation completed.");
        } catch (Exception e) {
            log.error("Partition management job failed", e);
        }
    }
}
