-- 澄客 CRM 用户管理 PostgreSQL 初始化脚本
-- 对应 Spec：docs/modules/modules/system/user/UserController.spec.md
-- 角色目录为用户模块读写依赖的最小表，角色管理模块落地后可接管维护。

CREATE TABLE IF NOT EXISTS chengke_crm.sys_role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    name varchar(50) NOT NULL,
    code varchar(50) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'active',
    built_in boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    created_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_by uuid NOT NULL,
    CONSTRAINT ck_sys_role_name CHECK (name = btrim(name) AND char_length(name) BETWEEN 1 AND 50),
    CONSTRAINT ck_sys_role_code CHECK (code ~ '^[a-z][a-z0-9_]{1,49}$'),
    CONSTRAINT ck_sys_role_status CHECK (status IN ('active', 'disabled'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_role_tenant_code
    ON chengke_crm.sys_role (tenant_id, lower(code));

CREATE TABLE IF NOT EXISTS chengke_crm.sys_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    name varchar(30) NOT NULL,
    username varchar(32) NOT NULL,
    password_hash varchar(100) NOT NULL,
    avatar_url varchar(500),
    mobile varchar(11) NOT NULL,
    email varchar(100),
    department_id uuid NOT NULL,
    role_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'active',
    force_password_change boolean NOT NULL DEFAULT true,
    remark varchar(200),
    version integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    created_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_by uuid NOT NULL,
    CONSTRAINT fk_sys_user_department
        FOREIGN KEY (tenant_id, department_id)
        REFERENCES chengke_crm.department (tenant_id, id),
    CONSTRAINT ck_sys_user_name
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 2 AND 30),
    CONSTRAINT ck_sys_user_username
        CHECK (username ~ '^[A-Za-z][A-Za-z0-9._-]{3,31}$'),
    CONSTRAINT ck_sys_user_mobile
        CHECK (mobile ~ '^1[3-9][0-9]{9}$'),
    CONSTRAINT ck_sys_user_email
        CHECK (
            email IS NULL
            OR (email = lower(btrim(email)) AND char_length(email) BETWEEN 3 AND 100)
        ),
    CONSTRAINT ck_sys_user_status
        CHECK (status IN ('active', 'disabled')),
    CONSTRAINT ck_sys_user_remark
        CHECK (remark IS NULL OR char_length(remark) <= 200),
    CONSTRAINT ck_sys_user_avatar
        CHECK (avatar_url IS NULL OR char_length(avatar_url) <= 500),
    CONSTRAINT ck_sys_user_version
        CHECK (version > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_user_tenant_username
    ON chengke_crm.sys_user (tenant_id, lower(username));

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_user_tenant_mobile
    ON chengke_crm.sys_user (tenant_id, mobile);

CREATE INDEX IF NOT EXISTS ix_sys_user_department
    ON chengke_crm.sys_user (tenant_id, department_id);

CREATE INDEX IF NOT EXISTS ix_sys_user_role
    ON chengke_crm.sys_user (tenant_id, role_id);

CREATE INDEX IF NOT EXISTS ix_sys_user_updated_at
    ON chengke_crm.sys_user (tenant_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS chengke_crm.user_audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    action varchar(30) NOT NULL,
    actor_id uuid NOT NULL,
    result varchar(20) NOT NULL,
    before_data jsonb,
    after_data jsonb,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT ck_user_audit_action
        CHECK (action IN ('create', 'update', 'enable', 'disable', 'reset_password')),
    CONSTRAINT ck_user_audit_result
        CHECK (result IN ('success', 'failure')),
    CONSTRAINT ck_user_audit_no_password
        CHECK (
            (before_data IS NULL OR (
                NOT (before_data ? 'password')
                AND NOT (before_data ? 'passwordHash')
                AND NOT (before_data ? 'initialPassword')
                AND NOT (before_data ? 'newPassword')
            ))
            AND (after_data IS NULL OR (
                NOT (after_data ? 'password')
                AND NOT (after_data ? 'passwordHash')
                AND NOT (after_data ? 'initialPassword')
                AND NOT (after_data ? 'newPassword')
            ))
        )
);

CREATE INDEX IF NOT EXISTS ix_user_audit_user
    ON chengke_crm.user_audit_log (tenant_id, user_id, occurred_at DESC);

COMMENT ON TABLE chengke_crm.sys_user IS '系统用户账号；密码凭据仅存不可逆散列。';
COMMENT ON TABLE chengke_crm.sys_role IS '用户模块依赖的最小角色目录，待角色管理模块接管。';
COMMENT ON TABLE chengke_crm.user_audit_log IS '用户管理审计；快照禁止包含密码凭据。';
