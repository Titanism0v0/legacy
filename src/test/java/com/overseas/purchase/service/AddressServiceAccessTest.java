package com.overseas.purchase.service;

import com.overseas.purchase.entity.Address;
import com.overseas.purchase.mapper.AddressMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceAccessTest {

    @Test
    void userCanUpdateOwnAddressAndForgedUserIdIsIgnored() {
        AddressMapper addressMapper = mock(AddressMapper.class);
        Address existing = address(1L, 10L);
        when(addressMapper.selectById(1L)).thenReturn(existing);
        AddressService service = new AddressService(addressMapper);
        Address update = address(1L, 999L);
        update.setReceiverName("new name");
        update.setIsDefault(0);

        service.updateAddress(update, 10L);

        assertThat(update.getUserId()).isEqualTo(10L);
        verify(addressMapper).updateById(update);
    }

    @Test
    void userCannotUpdateAnotherUsersAddress() {
        AddressMapper addressMapper = mock(AddressMapper.class);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 20L));
        AddressService service = new AddressService(addressMapper);
        Address update = address(1L, 10L);

        assertThatThrownBy(() -> service.updateAddress(update, 10L))
                .hasMessage("No permission");
        verify(addressMapper, never()).updateById(any());
    }

    @Test
    void userCanDeleteOwnAddress() {
        AddressMapper addressMapper = mock(AddressMapper.class);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 10L));
        AddressService service = new AddressService(addressMapper);

        service.deleteAddress(1L, 10L);

        verify(addressMapper).deleteById(1L);
    }

    @Test
    void userCannotDeleteAnotherUsersAddress() {
        AddressMapper addressMapper = mock(AddressMapper.class);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 20L));
        AddressService service = new AddressService(addressMapper);

        assertThatThrownBy(() -> service.deleteAddress(1L, 10L))
                .hasMessage("No permission");
        verify(addressMapper, never()).deleteById(1L);
    }

    private static Address address(Long id, Long userId) {
        Address address = new Address();
        address.setId(id);
        address.setUserId(userId);
        address.setIsDefault(0);
        address.setDeleted(0);
        return address;
    }
}
