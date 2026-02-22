CREATE TABLE `notification_seen` (
  `user_id` bigint NOT NULL,
  `last_seen_id` bigint NOT NULL DEFAULT 0,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_notification_seen_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
