-- Drop FK from interview.resume_id → resume.id
ALTER TABLE `interview`
    DROP FOREIGN KEY `fk_interview_resume`;
