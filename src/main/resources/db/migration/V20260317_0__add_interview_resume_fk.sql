ALTER TABLE `interview`
    ADD COLUMN `resume_id` BIGINT NULL AFTER `company_id`,
    ADD CONSTRAINT `fk_interview_resume`
        FOREIGN KEY (`resume_id`) REFERENCES `resume` (`id`)
        ON DELETE SET NULL;
