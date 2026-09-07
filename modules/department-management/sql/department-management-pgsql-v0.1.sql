-- 澄客 CRM 部门管理 PostgreSQL 14+ 初始化脚本
-- 对应 PRD：../docs/PRD-department-management-v0.1.md
-- 执行方式：psql -X -v ON_ERROR_STOP=1 -d <database> -f department-management-pgsql-v0.1.sql
--
-- 集成说明：
-- 1. 当前项目后端仍使用 MySQL，本脚本作为 PostgreSQL 独立交付物，不属于现有 Flyway 迁移。
-- 2. tenant_id、leader_user_id、created_by、updated_by 使用 UUID，但不引用尚未建立的租户/用户表。
-- 3. 用户、角色、业务数据等跨模块引用应调用 register_department_reference 登记；
--    protected=true 的有效引用会阻止部门删除。
-- 4. 应用层应优先调用本脚本提供的函数完成写操作，避免绕过层级、版本和审计规则。

\set ON_ERROR_STOP on

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS chengke_crm;

CREATE TABLE IF NOT EXISTS chengke_crm.department (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    parent_id uuid,
    name varchar(50) NOT NULL,
    code varchar(50) NOT NULL,
    node_type varchar(20) NOT NULL,
    leader_user_id uuid,
    sort_order integer NOT NULL DEFAULT 100,
    status varchar(20) NOT NULL DEFAULT 'active',
    remark varchar(200),
    path_ids uuid[] NOT NULL,
    depth integer NOT NULL,
    version integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    created_by uuid NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_by uuid NOT NULL,
    deleted_at timestamptz,
    deleted_by uuid,
    CONSTRAINT uq_department_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_department_parent
        FOREIGN KEY (tenant_id, parent_id)
        REFERENCES chengke_crm.department (tenant_id, id)
        DEFERRABLE INITIALLY IMMEDIATE,
    CONSTRAINT ck_department_name_trimmed
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 2 AND 50),
    CONSTRAINT ck_department_code_format
        CHECK (code ~ '^[a-z][a-z0-9_]{1,49}$'),
    CONSTRAINT ck_department_node_type
        CHECK (node_type IN ('group', 'department')),
    CONSTRAINT ck_department_type_parent
        CHECK (
            (node_type = 'group' AND parent_id IS NULL)
            OR (node_type = 'department' AND parent_id IS NOT NULL)
        ),
    CONSTRAINT ck_department_sort_order
        CHECK (sort_order BETWEEN 0 AND 9999),
    CONSTRAINT ck_department_status
        CHECK (status IN ('active', 'disabled')),
    CONSTRAINT ck_department_remark_length
        CHECK (remark IS NULL OR char_length(remark) <= 200),
    CONSTRAINT ck_department_depth
        CHECK (depth BETWEEN 0 AND 10),
    CONSTRAINT ck_department_path
        CHECK (
            cardinality(path_ids) = depth + 1
            AND path_ids[cardinality(path_ids)] = id
        ),
    CONSTRAINT ck_department_version
        CHECK (version > 0),
    CONSTRAINT ck_department_delete_pair
        CHECK (
            (deleted_at IS NULL AND deleted_by IS NULL)
            OR (deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_department_active_code
    ON chengke_crm.department (tenant_id, lower(code))
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_department_active_sibling_name
    ON chengke_crm.department (
        tenant_id,
        coalesce(parent_id, '00000000-0000-0000-0000-000000000000'::uuid),
        lower(name)
    )
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_department_active_group_root
    ON chengke_crm.department (tenant_id)
    WHERE node_type = 'group' AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_department_active_children
    ON chengke_crm.department (tenant_id, parent_id, sort_order, created_at, id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_department_active_leader
    ON chengke_crm.department (tenant_id, leader_user_id)
    WHERE deleted_at IS NULL AND leader_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_department_path_ids
    ON chengke_crm.department USING gin (path_ids);

CREATE TABLE IF NOT EXISTS chengke_crm.department_closure (
    tenant_id uuid NOT NULL,
    ancestor_id uuid NOT NULL,
    descendant_id uuid NOT NULL,
    distance integer NOT NULL,
    PRIMARY KEY (tenant_id, ancestor_id, descendant_id),
    CONSTRAINT fk_department_closure_ancestor
        FOREIGN KEY (tenant_id, ancestor_id)
        REFERENCES chengke_crm.department (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT fk_department_closure_descendant
        FOREIGN KEY (tenant_id, descendant_id)
        REFERENCES chengke_crm.department (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT ck_department_closure_distance CHECK (distance >= 0),
    CONSTRAINT ck_department_closure_self CHECK (
        (ancestor_id = descendant_id AND distance = 0)
        OR (ancestor_id <> descendant_id AND distance > 0)
    )
);

CREATE INDEX IF NOT EXISTS ix_department_closure_descendants
    ON chengke_crm.department_closure (tenant_id, descendant_id, distance, ancestor_id);

CREATE TABLE IF NOT EXISTS chengke_crm.department_reference (
    tenant_id uuid NOT NULL,
    department_id uuid NOT NULL,
    reference_type varchar(50) NOT NULL,
    reference_key varchar(100) NOT NULL,
    protected boolean NOT NULL DEFAULT true,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (tenant_id, department_id, reference_type, reference_key),
    CONSTRAINT fk_department_reference_department
        FOREIGN KEY (tenant_id, department_id)
        REFERENCES chengke_crm.department (tenant_id, id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_department_reference_type
        CHECK (reference_type ~ '^[a-z][a-z0-9_]{1,49}$'),
    CONSTRAINT ck_department_reference_key
        CHECK (char_length(btrim(reference_key)) BETWEEN 1 AND 100),
    CONSTRAINT ck_department_reference_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX IF NOT EXISTS ix_department_reference_lookup
    ON chengke_crm.department_reference (tenant_id, reference_type, reference_key);

CREATE TABLE IF NOT EXISTS chengke_crm.department_audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id uuid NOT NULL,
    department_id uuid NOT NULL,
    action varchar(30) NOT NULL,
    actor_id uuid NOT NULL,
    reason varchar(500),
    before_data jsonb,
    after_data jsonb,
    affected_department_count integer NOT NULL DEFAULT 1,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT ck_department_audit_action CHECK (
        action IN (
            'create',
            'update',
            'move',
            'enable',
            'disable',
            'soft_delete'
        )
    ),
    CONSTRAINT ck_department_audit_reason
        CHECK (reason IS NULL OR char_length(reason) <= 500),
    CONSTRAINT ck_department_audit_affected_count
        CHECK (affected_department_count > 0)
);

CREATE INDEX IF NOT EXISTS ix_department_audit_timeline
    ON chengke_crm.department_audit_log (tenant_id, department_id, occurred_at DESC, id DESC);

-- 负责部门行的统一初始化和不可变字段保护。
-- 参数 NEW/OLD 由 PostgreSQL 触发器传入；返回规范化后的部门行。
-- 副作用：自动计算 path_ids/depth，更新 updated_at，并在更新时递增 version。
CREATE OR REPLACE FUNCTION chengke_crm.department_row_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
DECLARE
    v_parent chengke_crm.department%ROWTYPE;
    v_allow_move boolean := coalesce(
        current_setting('chengke_crm.allow_department_move', true),
        'off'
    ) = 'on';
BEGIN
    NEW.name := btrim(NEW.name);
    NEW.code := lower(btrim(NEW.code));
    NEW.remark := nullif(btrim(NEW.remark), '');

    IF TG_OP = 'UPDATE' THEN
        IF NEW.id IS DISTINCT FROM OLD.id OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK001',
                MESSAGE = '部门 ID 和租户 ID 不可修改';
        END IF;

        IF NEW.code IS DISTINCT FROM OLD.code OR NEW.node_type IS DISTINCT FROM OLD.node_type THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK002',
                MESSAGE = '部门编码和节点类型创建后不可修改';
        END IF;

        IF NEW.parent_id IS DISTINCT FROM OLD.parent_id AND NOT v_allow_move THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK005',
                MESSAGE = '请通过 move_department 移动部门';
        END IF;
    END IF;

    IF NEW.node_type = 'group' THEN
        NEW.parent_id := NULL;
        NEW.path_ids := ARRAY[NEW.id]::uuid[];
        NEW.depth := 0;
    ELSIF TG_OP = 'INSERT' OR NEW.parent_id IS DISTINCT FROM OLD.parent_id THEN
        SELECT *
          INTO v_parent
          FROM chengke_crm.department
         WHERE tenant_id = NEW.tenant_id
           AND id = NEW.parent_id
           AND deleted_at IS NULL;

        IF NOT FOUND THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK006',
                MESSAGE = '上级部门不存在或不属于当前租户';
        END IF;

        IF v_parent.status <> 'active' THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK008',
                MESSAGE = '上级部门已停用，不能新增或移入部门';
        END IF;

        NEW.path_ids := v_parent.path_ids || NEW.id;
        NEW.depth := v_parent.depth + 1;

        IF NEW.depth > 10 THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK006',
                MESSAGE = '组织层级不能超过 10 层';
        END IF;
    ELSIF v_allow_move THEN
        IF cardinality(NEW.path_ids) <> NEW.depth + 1
           OR NEW.path_ids[cardinality(NEW.path_ids)] <> NEW.id
           OR NEW.depth > 10 THEN
            RAISE EXCEPTION USING
                ERRCODE = 'CK006',
                MESSAGE = '移动后的部门路径或层级无效';
        END IF;
    ELSE
        NEW.path_ids := OLD.path_ids;
        NEW.depth := OLD.depth;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        NEW.version := OLD.version + 1;
        NEW.updated_at := clock_timestamp();
    END IF;

    RETURN NEW;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.department_row_guard() IS
'触发器函数：规范化部门字段，保护不可变字段，计算路径和层级；更新时递增版本并刷新更新时间。';

DROP TRIGGER IF EXISTS trg_department_row_guard ON chengke_crm.department;
CREATE TRIGGER trg_department_row_guard
BEFORE INSERT OR UPDATE ON chengke_crm.department
FOR EACH ROW
EXECUTE FUNCTION chengke_crm.department_row_guard();

-- 在新增部门后建立自身闭包和全部祖先闭包。
-- 参数 NEW 由触发器传入；返回新增行。
-- 副作用：写入 department_closure。
CREATE OR REPLACE FUNCTION chengke_crm.department_closure_after_insert()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
    INSERT INTO chengke_crm.department_closure (
        tenant_id,
        ancestor_id,
        descendant_id,
        distance
    )
    VALUES (NEW.tenant_id, NEW.id, NEW.id, 0);

    IF NEW.parent_id IS NOT NULL THEN
        INSERT INTO chengke_crm.department_closure (
            tenant_id,
            ancestor_id,
            descendant_id,
            distance
        )
        SELECT tenant_id, ancestor_id, NEW.id, distance + 1
          FROM chengke_crm.department_closure
         WHERE tenant_id = NEW.tenant_id
           AND descendant_id = NEW.parent_id;
    END IF;

    RETURN NEW;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.department_closure_after_insert() IS
'触发器函数：新增部门后写入自身及全部祖先的闭包关系。';

DROP TRIGGER IF EXISTS trg_department_closure_after_insert ON chengke_crm.department;
CREATE TRIGGER trg_department_closure_after_insert
AFTER INSERT ON chengke_crm.department
FOR EACH ROW
EXECUTE FUNCTION chengke_crm.department_closure_after_insert();

-- 创建集团根节点或部门。
-- 参数依次为租户、父部门、名称、编码、类型、负责人、排序、状态、备注和操作人。
-- 返回新部门 UUID；副作用为新增部门、闭包关系和审计日志。
CREATE OR REPLACE FUNCTION chengke_crm.create_department(
    p_tenant_id uuid,
    p_parent_id uuid,
    p_name varchar,
    p_code varchar,
    p_node_type varchar,
    p_leader_user_id uuid,
    p_sort_order integer,
    p_status varchar,
    p_remark varchar,
    p_actor_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
AS $function$
DECLARE
    v_department chengke_crm.department%ROWTYPE;
BEGIN
    INSERT INTO chengke_crm.department (
        tenant_id,
        parent_id,
        name,
        code,
        node_type,
        leader_user_id,
        sort_order,
        status,
        remark,
        path_ids,
        depth,
        created_by,
        updated_by
    )
    VALUES (
        p_tenant_id,
        p_parent_id,
        p_name,
        p_code,
        p_node_type,
        p_leader_user_id,
        coalesce(p_sort_order, 100),
        coalesce(p_status, 'active'),
        p_remark,
        ARRAY[]::uuid[],
        0,
        p_actor_id,
        p_actor_id
    )
    RETURNING * INTO v_department;

    INSERT INTO chengke_crm.department_audit_log (
        tenant_id,
        department_id,
        action,
        actor_id,
        after_data
    )
    VALUES (
        p_tenant_id,
        v_department.id,
        'create',
        p_actor_id,
        to_jsonb(v_department) - 'deleted_by'
    );

    RETURN v_department.id;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.create_department(
    uuid, uuid, varchar, varchar, varchar, uuid, integer, varchar, varchar, uuid
) IS
'创建部门并返回 UUID；自动维护路径、闭包关系和审计日志。参数依次为租户、父节点、名称、编码、类型、负责人、排序、状态、备注、操作人。';

-- 更新部门的可编辑基本信息。
-- 参数包含期望版本，用于防止并发覆盖；返回更新后的新版本号。
-- 副作用：修改部门基本字段并写审计日志，不允许修改编码、类型、父节点和状态。
CREATE OR REPLACE FUNCTION chengke_crm.update_department(
    p_tenant_id uuid,
    p_department_id uuid,
    p_name varchar,
    p_leader_user_id uuid,
    p_sort_order integer,
    p_remark varchar,
    p_expected_version integer,
    p_actor_id uuid,
    p_reason varchar DEFAULT NULL
)
RETURNS integer
LANGUAGE plpgsql
AS $function$
DECLARE
    v_before chengke_crm.department%ROWTYPE;
    v_after chengke_crm.department%ROWTYPE;
BEGIN
    SELECT * INTO v_before
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
       AND deleted_at IS NULL
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '部门不存在';
    END IF;

    IF v_before.version <> p_expected_version THEN
        RAISE EXCEPTION USING ERRCODE = 'CK003', MESSAGE = '部门已被其他管理员修改';
    END IF;

    UPDATE chengke_crm.department
       SET name = p_name,
           leader_user_id = p_leader_user_id,
           sort_order = p_sort_order,
           remark = p_remark,
           updated_by = p_actor_id
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
    RETURNING * INTO v_after;

    INSERT INTO chengke_crm.department_audit_log (
        tenant_id,
        department_id,
        action,
        actor_id,
        reason,
        before_data,
        after_data
    )
    VALUES (
        p_tenant_id,
        p_department_id,
        'update',
        p_actor_id,
        p_reason,
        to_jsonb(v_before),
        to_jsonb(v_after)
    );

    RETURN v_after.version;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.update_department(
    uuid, uuid, varchar, uuid, integer, varchar, integer, uuid, varchar
) IS
'更新部门名称、负责人、排序和备注并返回新版本；校验期望版本，写入前后差异审计。';

-- 移动一个部门及其全部后代。
-- 参数包含租户、部门、新父节点、期望版本、操作人和原因；返回移动部门的新版本。
-- 副作用：原子更新父关系、整棵子树路径、闭包关系、版本和审计日志。
CREATE OR REPLACE FUNCTION chengke_crm.move_department(
    p_tenant_id uuid,
    p_department_id uuid,
    p_new_parent_id uuid,
    p_expected_version integer,
    p_actor_id uuid,
    p_reason varchar DEFAULT NULL
)
RETURNS integer
LANGUAGE plpgsql
AS $function$
DECLARE
    v_department chengke_crm.department%ROWTYPE;
    v_new_parent chengke_crm.department%ROWTYPE;
    v_after chengke_crm.department%ROWTYPE;
    v_max_subtree_distance integer;
    v_affected_count integer;
BEGIN
    SELECT * INTO v_department
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
       AND deleted_at IS NULL
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '待移动部门不存在';
    END IF;

    IF v_department.node_type = 'group' THEN
        RAISE EXCEPTION USING ERRCODE = 'CK009', MESSAGE = '集团根节点不可移动';
    END IF;

    IF v_department.version <> p_expected_version THEN
        RAISE EXCEPTION USING ERRCODE = 'CK003', MESSAGE = '部门已被其他管理员修改';
    END IF;

    SELECT * INTO v_new_parent
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_new_parent_id
       AND deleted_at IS NULL
     FOR UPDATE;

    IF NOT FOUND OR v_new_parent.status <> 'active' THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '目标父部门不存在或不可用';
    END IF;

    IF p_new_parent_id = p_department_id
       OR EXISTS (
            SELECT 1
              FROM chengke_crm.department_closure
             WHERE tenant_id = p_tenant_id
               AND ancestor_id = p_department_id
               AND descendant_id = p_new_parent_id
       ) THEN
        RAISE EXCEPTION USING ERRCODE = 'CK004', MESSAGE = '部门不能移动到自身或后代节点';
    END IF;

    IF v_department.parent_id = p_new_parent_id THEN
        RETURN v_department.version;
    END IF;

    SELECT max(distance), count(*)
      INTO v_max_subtree_distance, v_affected_count
      FROM chengke_crm.department_closure
     WHERE tenant_id = p_tenant_id
       AND ancestor_id = p_department_id;

    IF v_new_parent.depth + 1 + v_max_subtree_distance > 10 THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '移动后组织层级将超过 10 层';
    END IF;

    DELETE FROM chengke_crm.department_closure external_link
     WHERE external_link.tenant_id = p_tenant_id
       AND external_link.descendant_id IN (
            SELECT descendant_id
              FROM chengke_crm.department_closure
             WHERE tenant_id = p_tenant_id
               AND ancestor_id = p_department_id
       )
       AND external_link.ancestor_id NOT IN (
            SELECT descendant_id
              FROM chengke_crm.department_closure
             WHERE tenant_id = p_tenant_id
               AND ancestor_id = p_department_id
       );

    INSERT INTO chengke_crm.department_closure (
        tenant_id,
        ancestor_id,
        descendant_id,
        distance
    )
    SELECT p_tenant_id,
           parent_ancestors.ancestor_id,
           subtree.descendant_id,
           parent_ancestors.distance + 1 + subtree.distance
      FROM chengke_crm.department_closure parent_ancestors
      CROSS JOIN chengke_crm.department_closure subtree
     WHERE parent_ancestors.tenant_id = p_tenant_id
       AND parent_ancestors.descendant_id = p_new_parent_id
       AND subtree.tenant_id = p_tenant_id
       AND subtree.ancestor_id = p_department_id;

    PERFORM set_config('chengke_crm.allow_department_move', 'on', true);

    UPDATE chengke_crm.department department_row
       SET parent_id = CASE
               WHEN department_row.id = p_department_id THEN p_new_parent_id
               ELSE department_row.parent_id
           END,
           path_ids = v_new_parent.path_ids
               || department_row.path_ids[
                    array_position(department_row.path_ids, p_department_id):cardinality(department_row.path_ids)
                  ],
           depth = cardinality(
               v_new_parent.path_ids
               || department_row.path_ids[
                    array_position(department_row.path_ids, p_department_id):cardinality(department_row.path_ids)
                  ]
           ) - 1,
           updated_by = p_actor_id
     WHERE department_row.tenant_id = p_tenant_id
       AND department_row.id IN (
            SELECT descendant_id
              FROM chengke_crm.department_closure
             WHERE tenant_id = p_tenant_id
               AND ancestor_id = p_department_id
       );

    PERFORM set_config('chengke_crm.allow_department_move', 'off', true);

    SELECT * INTO v_after
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id;

    INSERT INTO chengke_crm.department_audit_log (
        tenant_id,
        department_id,
        action,
        actor_id,
        reason,
        before_data,
        after_data,
        affected_department_count
    )
    VALUES (
        p_tenant_id,
        p_department_id,
        'move',
        p_actor_id,
        p_reason,
        to_jsonb(v_department),
        to_jsonb(v_after),
        v_affected_count
    );

    RETURN v_after.version;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.move_department(
    uuid, uuid, uuid, integer, uuid, varchar
) IS
'原子移动部门子树并返回新版本；防止循环和超深，重建外部闭包、路径并记录影响审计。';

-- 启用或停用部门。
-- 参数 cascade 控制是否同时处理全部后代，expected_version 防止覆盖并发修改。
-- 返回受影响部门数；副作用为修改状态、递增版本并写审计日志。
CREATE OR REPLACE FUNCTION chengke_crm.set_department_status(
    p_tenant_id uuid,
    p_department_id uuid,
    p_status varchar,
    p_cascade boolean,
    p_expected_version integer,
    p_actor_id uuid,
    p_reason varchar DEFAULT NULL
)
RETURNS integer
LANGUAGE plpgsql
AS $function$
DECLARE
    v_department chengke_crm.department%ROWTYPE;
    v_parent_status varchar;
    v_affected_count integer;
BEGIN
    IF p_status NOT IN ('active', 'disabled') THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '部门状态只允许 active 或 disabled';
    END IF;

    SELECT * INTO v_department
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
       AND deleted_at IS NULL
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '部门不存在';
    END IF;

    IF v_department.node_type = 'group' THEN
        RAISE EXCEPTION USING ERRCODE = 'CK009', MESSAGE = '集团根节点不可启停';
    END IF;

    IF v_department.version <> p_expected_version THEN
        RAISE EXCEPTION USING ERRCODE = 'CK003', MESSAGE = '部门已被其他管理员修改';
    END IF;

    IF v_department.status = p_status THEN
        RETURN 0;
    END IF;

    IF p_status = 'disabled'
       AND NOT p_cascade
       AND EXISTS (
            SELECT 1
              FROM chengke_crm.department_closure closure_row
              JOIN chengke_crm.department descendant
                ON descendant.tenant_id = closure_row.tenant_id
               AND descendant.id = closure_row.descendant_id
             WHERE closure_row.tenant_id = p_tenant_id
               AND closure_row.ancestor_id = p_department_id
               AND closure_row.distance > 0
               AND descendant.deleted_at IS NULL
               AND descendant.status = 'active'
       ) THEN
        RAISE EXCEPTION USING
            ERRCODE = 'CK007',
            MESSAGE = '存在正常下级部门，请选择级联停用';
    END IF;

    IF p_status = 'active' THEN
        SELECT status INTO v_parent_status
          FROM chengke_crm.department
         WHERE tenant_id = p_tenant_id
           AND id = v_department.parent_id
           AND deleted_at IS NULL;

        IF v_parent_status IS DISTINCT FROM 'active' THEN
            RAISE EXCEPTION USING ERRCODE = 'CK008', MESSAGE = '上级部门未启用，不能启用当前部门';
        END IF;
    END IF;

    UPDATE chengke_crm.department department_row
       SET status = p_status,
           updated_by = p_actor_id
     WHERE department_row.tenant_id = p_tenant_id
       AND department_row.deleted_at IS NULL
       AND (
            department_row.id = p_department_id
            OR (
                p_cascade
                AND department_row.id IN (
                    SELECT descendant_id
                      FROM chengke_crm.department_closure
                     WHERE tenant_id = p_tenant_id
                       AND ancestor_id = p_department_id
                       AND distance > 0
                )
            )
       )
       AND department_row.status <> p_status;

    GET DIAGNOSTICS v_affected_count = ROW_COUNT;

    INSERT INTO chengke_crm.department_audit_log (
        tenant_id,
        department_id,
        action,
        actor_id,
        reason,
        before_data,
        after_data,
        affected_department_count
    )
    SELECT p_tenant_id,
           p_department_id,
           CASE WHEN p_status = 'active' THEN 'enable' ELSE 'disable' END,
           p_actor_id,
           p_reason,
           to_jsonb(v_department),
           to_jsonb(current_row),
           v_affected_count
      FROM chengke_crm.department current_row
     WHERE current_row.tenant_id = p_tenant_id
       AND current_row.id = p_department_id;

    RETURN v_affected_count;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.set_department_status(
    uuid, uuid, varchar, boolean, integer, uuid, varchar
) IS
'启用或停用部门并返回受影响节点数；可显式级联后代，校验根保护、父状态和期望版本并写审计。';

-- 登记用户、角色或业务数据对部门的跨模块引用。
-- 参数包含引用类型、外部键、是否阻止删除和 JSON 元数据；无返回值。
-- 副作用：幂等新增或更新 department_reference。
CREATE OR REPLACE FUNCTION chengke_crm.register_department_reference(
    p_tenant_id uuid,
    p_department_id uuid,
    p_reference_type varchar,
    p_reference_key varchar,
    p_protected boolean DEFAULT true,
    p_metadata jsonb DEFAULT '{}'::jsonb
)
RETURNS void
LANGUAGE plpgsql
AS $function$
BEGIN
    INSERT INTO chengke_crm.department_reference (
        tenant_id,
        department_id,
        reference_type,
        reference_key,
        protected,
        metadata
    )
    VALUES (
        p_tenant_id,
        p_department_id,
        lower(btrim(p_reference_type)),
        btrim(p_reference_key),
        p_protected,
        coalesce(p_metadata, '{}'::jsonb)
    )
    ON CONFLICT (tenant_id, department_id, reference_type, reference_key)
    DO UPDATE SET
        protected = EXCLUDED.protected,
        metadata = EXCLUDED.metadata,
        updated_at = clock_timestamp();
END;
$function$;

COMMENT ON FUNCTION chengke_crm.register_department_reference(
    uuid, uuid, varchar, varchar, boolean, jsonb
) IS
'幂等登记跨模块部门引用；protected=true 的记录会阻止软删除。参数为租户、部门、引用类型、外部键、保护标记和元数据。';

-- 注销已经解除的跨模块部门引用。
-- 参数唯一定位一条引用；返回是否实际删除记录。
-- 副作用：从 department_reference 删除对应关系。
CREATE OR REPLACE FUNCTION chengke_crm.unregister_department_reference(
    p_tenant_id uuid,
    p_department_id uuid,
    p_reference_type varchar,
    p_reference_key varchar
)
RETURNS boolean
LANGUAGE plpgsql
AS $function$
DECLARE
    v_deleted_count integer;
BEGIN
    DELETE FROM chengke_crm.department_reference
     WHERE tenant_id = p_tenant_id
       AND department_id = p_department_id
       AND reference_type = lower(btrim(p_reference_type))
       AND reference_key = btrim(p_reference_key);

    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    RETURN v_deleted_count = 1;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.unregister_department_reference(
    uuid, uuid, varchar, varchar
) IS
'注销一条跨模块部门引用并返回是否删除成功；调用方应在外部对象解除部门关系后执行。';

-- 软删除满足保护条件的叶子部门。
-- 参数包含期望版本、操作人和原因；返回删除后的新版本。
-- 副作用：标记 deleted_at/deleted_by、停用节点并写审计；保留闭包供历史追溯。
CREATE OR REPLACE FUNCTION chengke_crm.soft_delete_department(
    p_tenant_id uuid,
    p_department_id uuid,
    p_expected_version integer,
    p_actor_id uuid,
    p_reason varchar DEFAULT NULL
)
RETURNS integer
LANGUAGE plpgsql
AS $function$
DECLARE
    v_before chengke_crm.department%ROWTYPE;
    v_after chengke_crm.department%ROWTYPE;
BEGIN
    SELECT * INTO v_before
      FROM chengke_crm.department
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
       AND deleted_at IS NULL
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING ERRCODE = 'CK006', MESSAGE = '部门不存在';
    END IF;

    IF v_before.node_type = 'group' THEN
        RAISE EXCEPTION USING ERRCODE = 'CK009', MESSAGE = '集团根节点不可删除';
    END IF;

    IF v_before.version <> p_expected_version THEN
        RAISE EXCEPTION USING ERRCODE = 'CK003', MESSAGE = '部门已被其他管理员修改';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM chengke_crm.department child
         WHERE child.tenant_id = p_tenant_id
           AND child.parent_id = p_department_id
           AND child.deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION USING ERRCODE = 'CK010', MESSAGE = '部门存在下级部门，不能删除';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM chengke_crm.department_reference reference_row
         WHERE reference_row.tenant_id = p_tenant_id
           AND reference_row.department_id = p_department_id
           AND reference_row.protected
    ) THEN
        RAISE EXCEPTION USING ERRCODE = 'CK010', MESSAGE = '部门存在受保护引用，不能删除';
    END IF;

    UPDATE chengke_crm.department
       SET status = 'disabled',
           deleted_at = clock_timestamp(),
           deleted_by = p_actor_id,
           updated_by = p_actor_id
     WHERE tenant_id = p_tenant_id
       AND id = p_department_id
    RETURNING * INTO v_after;

    INSERT INTO chengke_crm.department_audit_log (
        tenant_id,
        department_id,
        action,
        actor_id,
        reason,
        before_data,
        after_data
    )
    VALUES (
        p_tenant_id,
        p_department_id,
        'soft_delete',
        p_actor_id,
        p_reason,
        to_jsonb(v_before),
        to_jsonb(v_after)
    );

    RETURN v_after.version;
END;
$function$;

COMMENT ON FUNCTION chengke_crm.soft_delete_department(
    uuid, uuid, integer, uuid, varchar
) IS
'软删除无下级且无受保护引用的部门并返回新版本；校验根保护和并发版本，停用节点并写审计。';

COMMENT ON TABLE chengke_crm.department IS
'租户内集团与部门主表；path_ids 包含从根到自身的 UUID 路径，业务写操作应优先调用存储函数。';

COMMENT ON TABLE chengke_crm.department_closure IS
'部门闭包表；distance=0 表示自身，用于高效查询祖先、后代和移动防环。';

COMMENT ON TABLE chengke_crm.department_reference IS
'跨模块部门引用登记表；用户、角色和业务模块用它向部门删除流程声明阻塞关系。';

COMMENT ON TABLE chengke_crm.department_audit_log IS
'部门创建、编辑、移动、启停和软删除的结构化审计日志。';

COMMIT;

-- 常用调用示例（按需替换 UUID 后单独执行）：
-- SELECT chengke_crm.create_department(
--   '10000000-0000-0000-0000-000000000001', NULL,
--   '澄客集团', 'chengke_group', 'group', NULL, 0, 'active', NULL,
--   '20000000-0000-0000-0000-000000000001'
-- );
--
-- SELECT d.id, d.name, d.depth, d.status
-- FROM chengke_crm.department d
-- WHERE d.tenant_id = '10000000-0000-0000-0000-000000000001'
--   AND d.deleted_at IS NULL
-- ORDER BY d.path_ids, d.sort_order, d.created_at;
