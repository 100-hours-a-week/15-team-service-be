-- Step 1: Backup existing table
RENAME TABLE `event_consume_log` TO `event_consume_log_old`;

-- Step 2: Create partitioned table with adjusted PK/UK to include created_at
CREATE TABLE `event_consume_log` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `created_at`       DATETIME(6)  NOT NULL,
  `updated_at`       DATETIME(6)  DEFAULT NULL,
  `consumer`         VARCHAR(100) NOT NULL,
  `event_id`         VARCHAR(100) NOT NULL,
  `queue_name`       VARCHAR(150) NOT NULL,
  `status`           ENUM('PROCESSING','SUCCESS') NOT NULL,
  `lease_expires_at` DATETIME(6)  DEFAULT NULL,
  `processed_at`     DATETIME(6)  DEFAULT NULL,
  PRIMARY KEY (`id`, `created_at`),
  UNIQUE KEY `UK_event_consume_log_consumer_event_id_created_at`
             (`consumer`, `event_id`, `created_at`),
  KEY `IDX_event_consume_log_queue_processed_at` (`queue_name`, `processed_at`),
  KEY `IDX_event_consume_log_status_lease`        (`status`, `lease_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
PARTITION BY RANGE (UNIX_TIMESTAMP(`created_at`)) (
  PARTITION p202601 VALUES LESS THAN (UNIX_TIMESTAMP('2026-02-01 00:00:00')),
  PARTITION p202602 VALUES LESS THAN (UNIX_TIMESTAMP('2026-03-01 00:00:00')),
  PARTITION p202603 VALUES LESS THAN (UNIX_TIMESTAMP('2026-04-01 00:00:00')),
  PARTITION p202604 VALUES LESS THAN (UNIX_TIMESTAMP('2026-05-01 00:00:00')),
  PARTITION p202605 VALUES LESS THAN (UNIX_TIMESTAMP('2026-06-01 00:00:00')),
  PARTITION p202606 VALUES LESS THAN (UNIX_TIMESTAMP('2026-07-01 00:00:00')),
  PARTITION pFuture VALUES LESS THAN MAXVALUE
);

-- Step 3: Migrate data
INSERT INTO `event_consume_log`
  SELECT `id`, `created_at`, `updated_at`, `consumer`, `event_id`,
         `queue_name`, `status`, `lease_expires_at`, `processed_at`
  FROM `event_consume_log_old`;

-- Step 4: Drop backup table
DROP TABLE `event_consume_log_old`;
