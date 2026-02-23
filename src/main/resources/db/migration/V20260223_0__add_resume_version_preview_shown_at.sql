ALTER TABLE resume_version
    ADD COLUMN preview_shown_at TIMESTAMP(6) NULL AFTER committed_at;
