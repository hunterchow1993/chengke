package com.chengke.chengkecrmbackend.modules.system.user.contract;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证生成的 OpenAPI 暴露用户管理核心路径，避免 Controller 与前端契约静默漂移。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserOpenApiContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DepartmentPersistencePort departmentPersistence;

    @MockitoBean
    private LeaderDirectoryPort leaderDirectory;

    @MockitoBean
    private UserPersistencePort userPersistence;

    @MockitoBean
    private AuthPersistencePort authPersistence;

    @Test
    @WithMockUser
    void shouldExposeAllUserActionPaths() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/system/users']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/{id}/status']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/batch-disable']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/{id}/reset-password']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/username-available']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/mobile-available']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/organization-tree']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/organization-tree/{nodeId}/children']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/organization-tree/search']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/assignable-departments']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/users/assignable-roles']").exists());
    }
}
