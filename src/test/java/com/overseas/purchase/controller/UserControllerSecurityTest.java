package com.overseas.purchase.controller;

import com.overseas.purchase.dto.UserProfileUpdateDTO;
import com.overseas.purchase.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserControllerSecurityTest {

    @Test
    void normalUserCannotUpdateAnotherUserProfile() {
        UserService userService = mock(UserService.class);
        UserController controller = new UserController(userService);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setId(20L);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(10L);
        when(request.getAttribute("role")).thenReturn("USER");

        com.overseas.purchase.common.Result<Void> result = controller.updateUser(dto, request);

        assertThat(result.getMessage()).isEqualTo("No permission");
        verify(userService, never()).updateUserProfile(any());
    }

    @Test
    void adminCanUpdateBasicProfileThroughWhitelistDto() {
        UserService userService = mock(UserService.class);
        UserController controller = new UserController(userService);
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setId(20L);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(10L);
        when(request.getAttribute("role")).thenReturn("ADMIN");

        com.overseas.purchase.common.Result<Void> result = controller.updateUser(dto, request);

        assertThat(result.getCode()).isEqualTo(200);
        verify(userService).updateUserProfile(dto);
    }

    @Test
    void resetPasswordByContactEndpointIsRemoved() throws Exception {
        MockMvc mockMvc = standaloneSetup(new UserController(mock(UserService.class))).build();

        mockMvc.perform(post("/user/reset-password-by-contact")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
