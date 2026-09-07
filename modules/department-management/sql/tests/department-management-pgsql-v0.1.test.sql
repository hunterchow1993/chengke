\set ON_ERROR_STOP on
\ir ../department-management-pgsql-v0.1.sql

BEGIN;

DO $test$
DECLARE
    v_tenant_id uuid := '10000000-0000-0000-0000-000000000001';
    v_actor_id uuid := '20000000-0000-0000-0000-000000000001';
    v_root_id uuid;
    v_sales_id uuid;
    v_east_id uuid;
    v_finance_id uuid;
    v_count integer;
BEGIN
    v_root_id := chengke_crm.create_department(
        v_tenant_id,
        NULL,
        '澄客集团',
        'chengke_group',
        'group',
        NULL,
        0,
        'active',
        '测试根节点',
        v_actor_id
    );

    v_sales_id := chengke_crm.create_department(
        v_tenant_id,
        v_root_id,
        '销售中心',
        'sales_center',
        'department',
        NULL,
        100,
        'active',
        NULL,
        v_actor_id
    );

    v_east_id := chengke_crm.create_department(
        v_tenant_id,
        v_sales_id,
        '华东销售部',
        'east_sales',
        'department',
        NULL,
        100,
        'active',
        NULL,
        v_actor_id
    );

    v_finance_id := chengke_crm.create_department(
        v_tenant_id,
        v_root_id,
        '财务部',
        'finance',
        'department',
        NULL,
        200,
        'active',
        NULL,
        v_actor_id
    );

    SELECT count(*)
      INTO v_count
      FROM chengke_crm.department_closure
     WHERE tenant_id = v_tenant_id
       AND ancestor_id = v_root_id
       AND descendant_id = v_east_id
       AND distance = 2;

    IF v_count <> 1 THEN
        RAISE EXCEPTION '根节点到孙节点的闭包关系不正确';
    END IF;

    PERFORM chengke_crm.move_department(
        v_tenant_id,
        v_sales_id,
        v_finance_id,
        1,
        v_actor_id,
        '数据库验收移动'
    );

    SELECT count(*)
      INTO v_count
      FROM chengke_crm.department_closure
     WHERE tenant_id = v_tenant_id
       AND ancestor_id = v_finance_id
       AND descendant_id = v_east_id
       AND distance = 2;

    IF v_count <> 1 THEN
        RAISE EXCEPTION '移动后闭包关系未正确重建';
    END IF;

    SELECT count(*)
      INTO v_count
      FROM chengke_crm.department
     WHERE tenant_id = v_tenant_id
       AND id = v_east_id
       AND path_ids = ARRAY[v_root_id, v_finance_id, v_sales_id, v_east_id]::uuid[]
       AND depth = 3
       AND version = 2;

    IF v_count <> 1 THEN
        RAISE EXCEPTION '移动后后代路径、层级或版本未正确更新';
    END IF;

    BEGIN
        PERFORM chengke_crm.move_department(
            v_tenant_id,
            v_finance_id,
            v_east_id,
            1,
            v_actor_id,
            '应被拒绝的循环移动'
        );
        RAISE EXCEPTION '循环移动本应被拒绝';
    EXCEPTION
        WHEN SQLSTATE 'CK004' THEN
            NULL;
    END;

    BEGIN
        PERFORM chengke_crm.create_department(
            v_tenant_id,
            v_finance_id,
            '销售中心',
            'sales_center_duplicate',
            'department',
            NULL,
            300,
            'active',
            NULL,
            v_actor_id
        );
        RAISE EXCEPTION '同级重名本应被拒绝';
    EXCEPTION
        WHEN unique_violation THEN
            NULL;
    END;

    BEGIN
        PERFORM chengke_crm.set_department_status(
            v_tenant_id,
            v_finance_id,
            'disabled',
            false,
            1,
            v_actor_id,
            '应被拒绝的非级联停用'
        );
        RAISE EXCEPTION '存在正常后代时非级联停用本应被拒绝';
    EXCEPTION
        WHEN SQLSTATE 'CK007' THEN
            NULL;
    END;

    PERFORM chengke_crm.register_department_reference(
        v_tenant_id,
        v_east_id,
        'user',
        'user_1001',
        true,
        '{"displayName":"测试用户"}'::jsonb
    );

    BEGIN
        PERFORM chengke_crm.soft_delete_department(
            v_tenant_id,
            v_east_id,
            2,
            v_actor_id,
            '应被引用保护拒绝'
        );
        RAISE EXCEPTION '存在保护引用时删除本应被拒绝';
    EXCEPTION
        WHEN SQLSTATE 'CK010' THEN
            NULL;
    END;
END;
$test$;

ROLLBACK;

SELECT 'department-management-pgsql-v0.1: PASS' AS result;
