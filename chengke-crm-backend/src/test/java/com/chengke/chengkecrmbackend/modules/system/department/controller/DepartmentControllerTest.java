package com.chengke.chengkecrmbackend.modules.system.department.controller;

import com.chengke.chengkecrmbackend.modules.system.department.application.DepartmentCommandService;
import com.chengke.chengkecrmbackend.modules.system.department.application.DepartmentQueryService;
import com.chengke.chengkecrmbackend.modules.system.department.controller.mapper.DepartmentApiMapper;
import com.chengke.chengkecrmbackend.shared.error.GlobalExceptionHandler;
import com.chengke.chengkecrmbackend.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证部门 Controller 的 HTTP 状态、Bean Validation 和统一错误契约。
 */
class DepartmentControllerTest {

    @Test
    void shouldReturnStableValidationErrorForInvalidCreateRequest() throws Exception {
        DepartmentApiMapper mapper = Mappers.getMapper(DepartmentApiMapper.class);
        var controller = new DepartmentController(mock(DepartmentQueryService.class),
                mock(DepartmentCommandService.class), mapper, mock(CurrentActorProvider.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/v1/system/departments/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"code\":\"BAD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_REQUEST_INVALID"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
