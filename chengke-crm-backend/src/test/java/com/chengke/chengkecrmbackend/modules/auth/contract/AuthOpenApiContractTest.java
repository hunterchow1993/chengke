package com.chengke.chengkecrmbackend.modules.auth.contract;

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
 * 验证生成的 OpenAPI 暴露登录模块核心路径，避免 Controller 与前端契约静默漂移。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthOpenApiContractTest {

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
    void shouldExposeAllAuthActionPaths() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/captcha']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/first-password-change']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/context']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout']").exists());
    }
}
