CREATE TABLE `outbox_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `event_type` varchar(120) NOT NULL,
  `aggregate_type` varchar(80) NOT NULL,
  `aggregate_id` varchar(80) NOT NULL,
  `payload` json NOT NULL,
  `status` enum('PENDING','PROCESSING','RETRY','PUBLISHED','FAILED') NOT NULL,
  `attempt_count` int NOT NULL,
  `max_attempts` int NOT NULL,
  `next_attempt_at` datetime(6) NOT NULL,
  `locked_at` datetime(6) DEFAULT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX_outbox_event_status_next_attempt_id` (`status`, `next_attempt_at`, `id`),
  KEY `IDX_outbox_event_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `event_consume_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `consumer` varchar(100) NOT NULL,
  `event_id` varchar(100) NOT NULL,
  `queue_name` varchar(150) NOT NULL,
  `status` enum('PROCESSING','SUCCESS') NOT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_event_consume_log_consumer_event_id` (`consumer`, `event_id`),
  KEY `IDX_event_consume_log_queue_processed_at` (`queue_name`, `processed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
