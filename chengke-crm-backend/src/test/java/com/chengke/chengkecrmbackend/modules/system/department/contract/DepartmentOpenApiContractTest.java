package com.chengke.chengkecrmbackend.modules.system.department.contract;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
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
 * 验证生成的 OpenAPI 暴露部门管理核心路径，避免 Controller 与前端契约静默漂移。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DepartmentOpenApiContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DepartmentPersistencePort persistence;

    @MockitoBean
    private LeaderDirectoryPort leaderDirectory;

    @MockitoBean
    private com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort userPersistence;

    @MockitoBean
    private AuthPersistencePort authPersistence;

    @Test
    @WithMockUser
    void shouldExposeAllDepartmentActionPaths() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/system/departments/tree']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/departments/{id}/move-preview']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/departments/{id}/move']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/departments/{id}/status-preview']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/departments/{id}/status']").exists());
    }
}
