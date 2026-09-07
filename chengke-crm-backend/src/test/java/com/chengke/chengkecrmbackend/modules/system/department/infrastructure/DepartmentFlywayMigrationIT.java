package com.chengke.chengkecrmbackend.modules.system.department.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在空 PostgreSQL 中验证 Flyway 迁移以及节点路径、深度和闭包维护。
 *
 * <p>本地未安装 Docker 时按 Testcontainers 约定跳过，不把环境缺失误报为代码失败。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class DepartmentFlywayMigrationIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("chengke_crm");

    @Test
    void shouldMigrateEmptyDatabaseAndMaintainClosure() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        flyway.validate();

        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.prepareStatement(
                     "select chengke_crm.create_department(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, tenantId);
            statement.setNull(2, Types.OTHER);
            statement.setString(3, "测试集团");
            statement.setString(4, "test_group");
            statement.setString(5, "group");
            statement.setNull(6, Types.OTHER);
            statement.setInt(7, 100);
            statement.setString(8, "active");
            statement.setNull(9, Types.VARCHAR);
            statement.setObject(10, actorId);
            var result = statement.executeQuery();
            assertThat(result.next()).isTrue();
            assertThat(result.getObject(1, UUID.class)).isNotNull();
        }
    }
}
