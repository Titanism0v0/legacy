package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.Address;
import com.overseas.purchase.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收货地址服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressMapper addressMapper;
    
    /**
     * 添加收货地址
     */
    public void addAddress(Address address) {
        // 如果设置为默认地址，取消其他默认地址
        if (address.getIsDefault() == 1) {
            List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, address.getUserId())
                    .eq(Address::getIsDefault, 1)
                    .eq(Address::getDeleted, 0)
            );
            addresses.forEach(addr -> {
                addr.setIsDefault(0);
                addressMapper.updateById(addr);
            });
        }
        
        addressMapper.insert(address);
    }
    
    /**
     * 查询用户的收货地址列表
     */
    public List<Address> getAddressList(Long userId) {
        return addressMapper.selectList(
            new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getDeleted, 0)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime)
        );
    }
    
    /**
     * 根据ID查询收货地址
     */
    public Address getAddressById(Long id) {
        return addressMapper.selectById(id);
    }
    
    /**
     * 更新收货地址
     */
    public void updateAddress(Address address, Long userId) {
        Address existing = addressMapper.selectById(address.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new RuntimeException("Address does not exist");
        }
        if (!userId.equals(existing.getUserId())) {
            throw new RuntimeException("No permission");
        }
        address.setUserId(userId);

        // 如果设置为默认地址，取消其他默认地址
        if (address.getIsDefault() == 1) {
            List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, 1)
                    .eq(Address::getDeleted, 0)
            );
            addresses.forEach(addr -> {
                if (!addr.getId().equals(address.getId())) {
                    addr.setIsDefault(0);
                    addressMapper.updateById(addr);
                }
            });
        }
        
        addressMapper.updateById(address);
    }
    
    /**
     * 删除收货地址
     */
    public void deleteAddress(Long id, Long userId) {
        Address existing = addressMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new RuntimeException("Address does not exist");
        }
        if (!userId.equals(existing.getUserId())) {
            throw new RuntimeException("No permission");
        }
        addressMapper.deleteById(id);
    }
}
