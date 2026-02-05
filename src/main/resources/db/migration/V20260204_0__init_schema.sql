--
-- Table structure for table `auth`
--

DROP TABLE IF EXISTS `auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `access_token` text,
  `provider` enum('GITHUB') NOT NULL,
  `provider_user_id` varchar(255) NOT NULL,
  `provider_username` varchar(255) DEFAULT NULL,
  `token_expires_at` datetime(6) DEFAULT NULL,
  `token_scopes` text,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKox9lr2lxr8h7undhmflx4xqky` (`user_id`),
  CONSTRAINT `FKpv45mvdt7km1gvrtn5f9rsvd7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `companies`
--

DROP TABLE IF EXISTS `companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ideal_talent` text,
  `is_verified` bit(1) NOT NULL,
  `name` varchar(200) NOT NULL,
  `preferred` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interview`
--

DROP TABLE IF EXISTS `interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interview` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_session_id` varchar(36) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `interview_type` enum('BEHAVIORAL','TECHNICAL') NOT NULL,
  `name` varchar(255) NOT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `total_feedback` text,
  `company_id` bigint DEFAULT NULL,
  `position_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKta0v5dg8yt6vwfd0bwb4hddr5` (`company_id`),
  KEY `FK8baax448gcv9tx4k2pybnjkvm` (`position_id`),
  KEY `FKf8b4fu9j2ampltg1rnirdy19h` (`user_id`),
  CONSTRAINT `FK8baax448gcv9tx4k2pybnjkvm` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`),
  CONSTRAINT `FKf8b4fu9j2ampltg1rnirdy19h` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKta0v5dg8yt6vwfd0bwb4hddr5` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interview_message`
--

DROP TABLE IF EXISTS `interview_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interview_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_responded_at` datetime(6) DEFAULT NULL,
  `answer` text,
  `answer_input_type` enum('AUDIO','TEXT') DEFAULT NULL,
  `answered_at` datetime(6) DEFAULT NULL,
  `asked_at` datetime(6) NOT NULL,
  `question` text,
  `turn_no` int NOT NULL,
  `interview_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7diqmimpybjxonti15dqoln7m` (`interview_id`),
  CONSTRAINT `FK7diqmimpybjxonti15dqoln7m` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `payload` json NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `type` enum('CHAT','RESUME') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnk4ftb5am9ubmkv1661h15ds9` (`user_id`),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `policy_agreement`
--

DROP TABLE IF EXISTS `policy_agreement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `policy_agreement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `agreed_at` datetime(6) DEFAULT NULL,
  `document` varchar(1024) NOT NULL,
  `policy_type` enum('PHONE_PRIVACY','PRIVACY') NOT NULL,
  `policy_version` varchar(60) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKclkx5vgiolotu9c4rut083pau` (`user_id`),
  CONSTRAINT `FKclkx5vgiolotu9c4rut083pau` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `position_chat_attachment`
--

DROP TABLE IF EXISTS `position_chat_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position_chat_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `file_type` enum('IMAGE') NOT NULL,
  `file_url` text NOT NULL,
  `order_no` int NOT NULL,
  `message_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm6b6nwumv2kdvdxufsopc3xby` (`message_id`),
  CONSTRAINT `FKm6b6nwumv2kdvdxufsopc3xby` FOREIGN KEY (`message_id`) REFERENCES `position_chat_message` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `position_chat_chatroom`
--

DROP TABLE IF EXISTS `position_chat_chatroom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position_chat_chatroom` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `position_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKrl7y6piue1s1tw7sicadcfgb5` (`position_id`),
  CONSTRAINT `FKilmcyf55yrwrjkfej30685pkb` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `position_chat_message`
--

DROP TABLE IF EXISTS `position_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message` text,
  `message_type` enum('IMAGE','MIXED','TEXT') DEFAULT NULL,
  `role` enum('CHAT') NOT NULL,
  `status` enum('FAILED','PENDING','SENT') NOT NULL,
  `chatroom_id` bigint NOT NULL,
  `mention_user_id` bigint DEFAULT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKksuwmvf7twb9xse8icxwnug2c` (`chatroom_id`),
  KEY `FKg7bp1vaw1sbb7257hxwkh41eg` (`mention_user_id`),
  KEY `FK2fr8d3tkeo7xfdimqg5yki7l4` (`sender_id`),
  CONSTRAINT `FK2fr8d3tkeo7xfdimqg5yki7l4` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKg7bp1vaw1sbb7257hxwkh41eg` FOREIGN KEY (`mention_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKksuwmvf7twb9xse8icxwnug2c` FOREIGN KEY (`chatroom_id`) REFERENCES `position_chat_chatroom` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `position_chat_usernumber`
--

DROP TABLE IF EXISTS `position_chat_usernumber`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position_chat_usernumber` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `no` int NOT NULL,
  `chatroom_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrrng5kykg31xplgp5659yofbg` (`chatroom_id`),
  KEY `FK49i0lbmqwqqj7d5gulk5cepqa` (`user_id`),
  CONSTRAINT `FK49i0lbmqwqqj7d5gulk5cepqa` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrrng5kykg31xplgp5659yofbg` FOREIGN KEY (`chatroom_id`) REFERENCES `position_chat_chatroom` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `positions`
--

DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `token_hash` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resume`
--

DROP TABLE IF EXISTS `resume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `current_version_no` int DEFAULT NULL,
  `name` varchar(30) NOT NULL,
  `company_id` bigint DEFAULT NULL,
  `position_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm2p995iawt7o4w4ogofp9n6y2` (`company_id`),
  KEY `FKt0ec3qp54icryggqtbp4m0sl0` (`position_id`),
  KEY `FKpv0whudowxosfu792veo6s2c0` (`user_id`),
  CONSTRAINT `FKm2p995iawt7o4w4ogofp9n6y2` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`),
  CONSTRAINT `FKpv0whudowxosfu792veo6s2c0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKt0ec3qp54icryggqtbp4m0sl0` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resume_version`
--

DROP TABLE IF EXISTS `resume_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_task_id` varchar(36) DEFAULT NULL,
  `committed_at` datetime(6) DEFAULT NULL,
  `content` json NOT NULL,
  `error_log` text,
  `finished_at` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` enum('DRAFT','FAILED','PROCESSING','QUEUED','SUCCEEDED') NOT NULL,
  `version_no` int NOT NULL,
  `resume_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKng93ks2r5uro0gtq29djg0gs7` (`resume_id`),
  CONSTRAINT `FKng93ks2r5uro0gtq29djg0gs7` FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resume_version_message`
--

DROP TABLE IF EXISTS `resume_version_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resume_version_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `ai_task_id` varchar(36) DEFAULT NULL,
  `answer` text,
  `question` text NOT NULL,
  `turn_no` int NOT NULL,
  `resume_version_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKln4in6l5u1fgn7vc0a4014pv4` (`resume_version_id`),
  CONSTRAINT `FKln4in6l5u1fgn7vc0a4014pv4` FOREIGN KEY (`resume_version_id`) REFERENCES `resume_version` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `uploads`
--

DROP TABLE IF EXISTS `uploads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `uploads` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `content_type` varchar(100) NOT NULL,
  `etag` varchar(200) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint NOT NULL,
  `owner_user_id` bigint NOT NULL,
  `purpose` enum('CHAT_ATTACHMENT','INTERVIEW_AUDIO','PROFILE_IMAGE') NOT NULL,
  `s3_key` varchar(512) NOT NULL,
  `status` enum('EXPIRED','PENDING','UPLOADED') NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_settings`
--

DROP TABLE IF EXISTS `user_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_settings` (
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `interview_resume_defaults_enabled` bit(1) NOT NULL,
  `notification_enabled` bit(1) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK8v82nj88rmai0nyck19f873dw` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `name` varchar(10) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `profile_image_url` varchar(1024) DEFAULT NULL,
  `status` enum('ACTIVE','BLOCKED','INACTIVE','PENDING') NOT NULL,
  `position_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6ph6xiiydudp6umjf2xckbbmi` (`position_id`),
  CONSTRAINT `FK6ph6xiiydudp6umjf2xckbbmi` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'commitme'
--

--
-- Dumping routines for database 'commitme'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-04 20:36:16
