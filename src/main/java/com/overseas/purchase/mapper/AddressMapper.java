package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址Mapper接口
 * 
 * @author System
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
