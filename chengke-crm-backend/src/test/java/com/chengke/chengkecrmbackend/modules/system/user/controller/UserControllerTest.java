package com.chengke.chengkecrmbackend.modules.system.user.controller;

import com.chengke.chengkecrmbackend.modules.system.user.application.UserCommandService;
import com.chengke.chengkecrmbackend.modules.system.user.application.UserQueryService;
import com.chengke.chengkecrmbackend.modules.system.user.controller.mapper.UserApiMapper;
import com.chengke.chengkecrmbackend.shared.error.GlobalExceptionHandler;
import com.chengke.chengkecrmbackend.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.RequestContextFilter;

import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证用户 Controller 的 HTTP 状态、Bean Validation 和统一错误契约。
 */
class UserControllerTest {

    @Test
    void shouldReturnStableValidationErrorForInvalidCreateRequest() throws Exception {
        UserApiMapper mapper = Mappers.getMapper(UserApiMapper.class);
        var controller = new UserController(mock(UserQueryService.class), mock(UserCommandService.class),
                mapper, mock(CurrentActorProvider.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestContextFilter())
                .build();

        mvc.perform(post("/api/v1/system/users/")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"name\":\"A\",\"username\":\"1bad\",\"mobile\":\"123\",\"initialPassword\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_REQUEST_INVALID"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
