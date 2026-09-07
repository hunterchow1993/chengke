-- 登录会话、验证码、失败计数与登录审计。

CREATE TABLE IF NOT EXISTS chengke_crm.auth_session (
    id uuid PRIMARY KEY,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    remember_me boolean NOT NULL DEFAULT false,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX IF NOT EXISTS ix_auth_session_user
    ON chengke_crm.auth_session (tenant_id, user_id, revoked_at);

CREATE TABLE IF NOT EXISTS chengke_crm.auth_captcha (
    token varchar(64) PRIMARY KEY,
    code_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);

CREATE TABLE IF NOT EXISTS chengke_crm.auth_login_failure (
    account_key varchar(32) PRIMARY KEY,
    fail_count integer NOT NULL DEFAULT 0,
    window_started_at timestamptz,
    locked_until timestamptz
);

CREATE TABLE IF NOT EXISTS chengke_crm.auth_login_audit (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id uuid,
    user_id uuid,
    account_key varchar(32),
    result varchar(20) NOT NULL,
    error_code varchar(40),
    ip varchar(64),
    user_agent varchar(300),
    session_created boolean NOT NULL DEFAULT false,
    session_revoked boolean NOT NULL DEFAULT false,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT ck_auth_login_audit_result CHECK (result IN ('success', 'failure'))
);

CREATE INDEX IF NOT EXISTS ix_auth_login_audit_time
    ON chengke_crm.auth_login_audit (occurred_at DESC);

COMMENT ON TABLE chengke_crm.auth_session IS '登录会话，jti 对应该表主键，撤销后 JWT 不可再用。';
COMMENT ON TABLE chengke_crm.auth_login_audit IS '登录审计；禁止写入密码、验证码原文和完整凭证。';
