ALTER TABLE `event_consume_log`
    ADD COLUMN `lease_expires_at` datetime(6) DEFAULT NULL AFTER `status`;

UPDATE `event_consume_log`
   SET `lease_expires_at` = DATE_ADD(`created_at`, INTERVAL 30 SECOND)
 WHERE `status` = 'PROCESSING'
   AND `lease_expires_at` IS NULL;

ALTER TABLE `event_consume_log`
    ADD KEY `IDX_event_consume_log_status_lease` (`status`, `lease_expires_at`);
