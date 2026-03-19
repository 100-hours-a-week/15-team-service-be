package com.sipomeokjo.commitme.batch.partition;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PartitionManagementService {

    private static final Logger log = LoggerFactory.getLogger(PartitionManagementService.class);
    private static final String TABLE_NAME = "event_consume_log";
    private static final DateTimeFormatter PARTITION_NAME_FMT =
            DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter BOUNDARY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public PartitionManagementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void rotate(int monthsAhead, int monthsToKeep) {
        List<String> existing = fetchExistingPartitionNames();
        addFuturePartitions(existing, monthsAhead);
        dropOldPartitions(existing, monthsToKeep);
    }

    private List<String> fetchExistingPartitionNames() {
        return jdbcTemplate.queryForList(
                "SELECT PARTITION_NAME FROM information_schema.PARTITIONS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                        + "AND PARTITION_NAME IS NOT NULL "
                        + "ORDER BY PARTITION_ORDINAL_POSITION",
                String.class,
                TABLE_NAME);
    }

    private void addFuturePartitions(List<String> existing, int monthsAhead) {
        LocalDate now = LocalDate.now();
        List<String> toAdd = new ArrayList<>();
        List<String> boundaries = new ArrayList<>();

        for (int i = 0; i <= monthsAhead; i++) {
            LocalDate month = now.plusMonths(i).withDayOfMonth(1);
            String name = "p" + month.format(PARTITION_NAME_FMT);
            if (!existing.contains(name) && !"pFuture".equals(name)) {
                LocalDate nextMonth = month.plusMonths(1);
                String boundary = nextMonth.atStartOfDay().format(BOUNDARY_FMT);
                toAdd.add(name);
                boundaries.add(boundary);
            }
        }

        if (toAdd.isEmpty()) {
            log.debug("No new partitions to add for {}", TABLE_NAME);
            return;
        }

        StringBuilder reorganize = new StringBuilder();
        reorganize.append("ALTER TABLE `").append(TABLE_NAME).append("` ");
        reorganize.append("REORGANIZE PARTITION `pFuture` INTO (");

        for (int i = 0; i < toAdd.size(); i++) {
            reorganize
                    .append("PARTITION `")
                    .append(toAdd.get(i))
                    .append("` VALUES LESS THAN (UNIX_TIMESTAMP('")
                    .append(boundaries.get(i))
                    .append("')),");
        }
        reorganize.append("PARTITION `pFuture` VALUES LESS THAN MAXVALUE)");

        log.info("Adding partitions {} to {}", toAdd, TABLE_NAME);
        jdbcTemplate.execute(reorganize.toString());
    }

    private void dropOldPartitions(List<String> existing, int monthsToKeep) {
        LocalDate cutoff = LocalDate.now().minusMonths(monthsToKeep).withDayOfMonth(1);

        for (String name : existing) {
            if (!name.startsWith("p") || name.equals("pFuture")) {
                continue;
            }
            String monthStr = name.substring(1); // e.g. "202601"
            if (monthStr.length() != 6) {
                continue;
            }
            try {
                int year = Integer.parseInt(monthStr.substring(0, 4));
                int month = Integer.parseInt(monthStr.substring(4, 6));
                LocalDate partitionMonth = LocalDate.of(year, month, 1);
                if (partitionMonth.isBefore(cutoff)) {
                    log.info("Dropping old partition {} from {}", name, TABLE_NAME);
                    jdbcTemplate.execute(
                            "ALTER TABLE `" + TABLE_NAME + "` DROP PARTITION `" + name + "`");
                }
            } catch (NumberFormatException e) {
                log.warn("Skipping unrecognized partition name: {}", name);
            }
        }
    }
}
