-- Align time columns with Instant usage for auth/refresh/policy/resume_version.
alter table auth
    modify column token_expires_at timestamp(6) null;

alter table refresh_tokens
    modify column expires_at timestamp(6) not null,
    modify column revoked_at timestamp(6) null;

alter table policy_agreement
    modify column agreed_at timestamp(6) null;

alter table resume_version
    modify column started_at timestamp(6) null,
    modify column finished_at timestamp(6) null,
    modify column committed_at timestamp(6) null;
