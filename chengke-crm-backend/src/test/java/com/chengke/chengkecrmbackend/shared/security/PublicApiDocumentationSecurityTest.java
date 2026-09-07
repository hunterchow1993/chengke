package com.chengke.chengkecrmbackend.shared.security;

import com.chengke.chengkecrmbackend.modules.auth.application.port.AuthPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.DepartmentPersistencePort;
import com.chengke.chengkecrmbackend.modules.system.department.application.port.LeaderDirectoryPort;
import com.chengke.chengkecrmbackend.modules.system.user.application.port.UserPersistencePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 OpenAPI / Swagger 可匿名访问，登录验证码公开，受保护 API 返回 401 且不跳转登录页。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicApiDocumentationSecurityTest {

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
    void shouldAllowAnonymousOpenApiDocsWithoutLoginRedirect() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousSwaggerUiWithoutLoginRedirect() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousCaptcha() throws Exception {
        mvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.captchaToken").isNotEmpty())
                .andExpect(jsonPath("$.data.imageBase64").isNotEmpty());
    }

    @Test
    void shouldRejectProtectedApiWithUnauthorizedInsteadOfLoginRedirect() throws Exception {
        mvc.perform(get("/api/v1/system/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
