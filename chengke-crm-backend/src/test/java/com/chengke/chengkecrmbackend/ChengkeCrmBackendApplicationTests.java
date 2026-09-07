package com.chengke.chengkecrmbackend;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 验证不连接外部 PostgreSQL 的基础 Spring 容器可以完成装配。
 */
@SpringBootTest
class ChengkeCrmBackendApplicationTests {

    @MockitoBean
    private DepartmentPersistencePort departmentPersistencePort;

    @MockitoBean
    private LeaderDirectoryPort leaderDirectoryPort;

    @MockitoBean
    private com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort userPersistencePort;

    @MockitoBean
    private AuthPersistencePort authPersistencePort;

    @Test
    void contextLoads() {
    }

}
