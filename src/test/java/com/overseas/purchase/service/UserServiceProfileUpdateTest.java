package com.overseas.purchase.service;

import com.overseas.purchase.dto.UserProfileUpdateDTO;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.AddressMapper;
import com.overseas.purchase.mapper.CartMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceProfileUpdateTest {

    @Test
    void profileUpdateOnlyChangesAllowedFields() {
        UserMapper userMapper = mock(UserMapper.class);
        User existing = existingUser();
        when(userMapper.selectById(10L)).thenReturn(existing);
        UserService service = userService(userMapper);
        UserProfileUpdateDTO dto = profileUpdate(10L);

        service.updateUserProfile(dto);

        assertThat(existing.getNickname()).isEqualTo("new nick");
        assertThat(existing.getAvatar()).isEqualTo("/avatar.png");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getPhone()).isEqualTo("13800000000");
        assertThat(existing.getCountry()).isEqualTo("USD");
        assertThat(existing.getRole()).isEqualTo("USER");
        assertThat(existing.getStatus()).isEqualTo(1);
        assertThat(existing.getKycStatus()).isEqualTo("UNSUBMITTED");
        assertThat(existing.getKycFiles()).isEqualTo("{old}");
        assertThat(existing.getKycRemark()).isEqualTo("old remark");
        assertThat(existing.getPassword()).isEqualTo("old-password");
        assertThat(existing.getDeleted()).isEqualTo(0);
        verify(userMapper).updateById(existing);
    }

    @Test
    void profileUpdateRejectsMissingUser() {
        UserMapper userMapper = mock(UserMapper.class);
        UserService service = userService(userMapper);

        assertThatThrownBy(() -> service.updateUserProfile(profileUpdate(404L)))
                .hasMessage("User does not exist");
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void profileUpdateRejectsDeletedUser() {
        UserMapper userMapper = mock(UserMapper.class);
        User deleted = existingUser();
        deleted.setDeleted(1);
        when(userMapper.selectById(10L)).thenReturn(deleted);
        UserService service = userService(userMapper);

        assertThatThrownBy(() -> service.updateUserProfile(profileUpdate(10L)))
                .hasMessage("User does not exist");
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    private static UserProfileUpdateDTO profileUpdate(Long id) {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setId(id);
        dto.setNickname("new nick");
        dto.setAvatar("/avatar.png");
        dto.setEmail("new@example.com");
        dto.setPhone("13800000000");
        dto.setCountry("USD");
        return dto;
    }

    private static User existingUser() {
        User user = new User();
        user.setId(10L);
        user.setUsername("buyer");
        user.setPassword("old-password");
        user.setNickname("old nick");
        user.setAvatar("/old.png");
        user.setEmail("old@example.com");
        user.setPhone("13900000000");
        user.setCountry("CNY");
        user.setRole("USER");
        user.setStatus(1);
        user.setKycStatus("UNSUBMITTED");
        user.setKycFiles("{old}");
        user.setKycRemark("old remark");
        user.setDeleted(0);
        return user;
    }

    private static UserService userService(UserMapper userMapper) {
        return new UserService(
                userMapper,
                mock(OrderMapper.class),
                mock(CartMapper.class),
                mock(ProductMapper.class),
                mock(AddressMapper.class),
                mock(com.overseas.purchase.common.JwtUtil.class),
                mock(LegalService.class),
                mock(AdminAuditModerationService.class));
    }
}
