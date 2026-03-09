alter table refresh_tokens
    add unique key UK_refresh_tokens_token_hash (`token_hash`),
    add key IDX_refresh_tokens_user_revoked (`user_id`, `revoked_at`);
