CREATE TABLE `resume_profiles` (
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `phone_country_code` varchar(8) DEFAULT NULL,
  `phone_national_number` varchar(30) DEFAULT NULL,
  `summary` text,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FK_resume_profiles_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tech_stacks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `name_normalized` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_tech_stacks_name` (`name`),
  UNIQUE KEY `UK_tech_stacks_name_normalized` (`name_normalized`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_tech_stacks` (
  `user_id` bigint NOT NULL,
  `tech_stack_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `tech_stack_id`),
  KEY `IDX_user_tech_stacks_tech_stack_id` (`tech_stack_id`),
  CONSTRAINT `FK_user_tech_stacks_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_user_tech_stacks_tech_stack`
    FOREIGN KEY (`tech_stack_id`) REFERENCES `tech_stacks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_experiences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `company_name` varchar(200) NOT NULL,
  `position_title` varchar(120) NOT NULL,
  `department_name` varchar(120) DEFAULT NULL,
  `employment_type` enum('INTERN','CONTRACT','FULL_TIME','BUSINESS','FREELANCE') NOT NULL,
  `start_year` smallint NOT NULL,
  `start_month` tinyint NOT NULL,
  `end_year` smallint DEFAULT NULL,
  `end_month` tinyint DEFAULT NULL,
  `is_current` bit(1) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`),
  KEY `IDX_user_experiences_user_id` (`user_id`),
  CONSTRAINT `FK_user_experiences_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_educations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `type` enum('PRIVATE','HIGH_SCHOOL','COLLEGE_ASSOCIATE','COLLEGE_BACHELOR','MASTER','PHD') NOT NULL,
  `institution` varchar(200) NOT NULL,
  `major_field` varchar(200) NOT NULL,
  `status` enum('GRADUATED','DEFERRED','ENROLLED','DROPPED','COMPLETED') NOT NULL,
  `start_year` smallint NOT NULL,
  `start_month` tinyint NOT NULL,
  `end_year` smallint DEFAULT NULL,
  `end_month` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX_user_educations_user_id` (`user_id`),
  CONSTRAINT `FK_user_educations_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_activities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `activity_name` varchar(200) NOT NULL,
  `organization` varchar(200) NOT NULL,
  `activity_year` smallint NOT NULL,
  `description` text,
  PRIMARY KEY (`id`),
  KEY `IDX_user_activities_user_id` (`user_id`),
  CONSTRAINT `FK_user_activities_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_certificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `type` enum('CERTIFICATE','LANGUAGE') NOT NULL,
  `title` varchar(200) NOT NULL,
  `grade_or_score` varchar(120) DEFAULT NULL,
  `issuer` varchar(200) DEFAULT NULL,
  `acquired_year` smallint NOT NULL,
  `acquired_month` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX_user_certificates_user_id` (`user_id`),
  CONSTRAINT `FK_user_certificates_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
