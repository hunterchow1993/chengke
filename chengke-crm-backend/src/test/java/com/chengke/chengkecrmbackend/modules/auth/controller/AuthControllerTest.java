package com.chengke.chengkecrmbackend.modules.auth.controller;

import com.chengke.chengkecrmbackend.modules.auth.application.AuthQueryService;
import com.chengke.chengkecrmbackend.modules.auth.application.LoginCommandService;
import com.chengke.chengkecrmbackend.modules.auth.application.result.LoginAttemptResult;
import com.chengke.chengkecrmbackend.modules.auth.controller.mapper.AuthApiMapper;
import com.chengke.chengkecrmbackend.shared.error.GlobalExceptionHandler;
import com.chengke.chengkecrmbackend.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.RequestContextFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证登录 Controller 的 HTTP 状态、Bean Validation 和业务失败仍为 200。
 */
class AuthControllerTest {

    @Test
    void shouldReturnInvalidRequestForBlankLoginBody() throws Exception {
        MockMvc mvc = mvc(mock(LoginCommandService.class));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldReturnOkWhenLoginBusinessFails() throws Exception {
        LoginCommandService commandService = mock(LoginCommandService.class);
        when(commandService.login(any())).thenReturn(LoginAttemptResult.failure("INVALID_CREDENTIALS", false));
        MockMvc mvc = mvc(commandService);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"admin\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("INVALID_CREDENTIALS"));
    }

    private MockMvc mvc(LoginCommandService commandService) {
        var controller = new AuthController(commandService, mock(AuthQueryService.class),
                new AuthApiMapper(), mock(CurrentActorProvider.class));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestContextFilter())
                .build();
    }
}
