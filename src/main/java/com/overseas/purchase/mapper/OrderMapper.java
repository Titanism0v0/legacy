package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.dto.OrderDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper接口
 * 
 * @author System
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    
    /**
     * 查询订单列表（包含商品和地址信息）
     */
    List<OrderDTO> selectOrderList(@Param("buyerId") Long buyerId,
                                   @Param("sellerId") Long sellerId,
                                   @Param("status") String status);
    
    /**
     * 根据ID查询订单详情
     */
    OrderDTO selectOrderById(@Param("id") Long id);
}
