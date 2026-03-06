package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 购物车Mapper接口
 * 
 * @author System
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
    
    /**
     * 恢复已删除的购物车记录并更新数量（忽略逻辑删除）
     */
    @Update("UPDATE cart SET deleted = 0, quantity = #{quantity}, update_time = NOW() WHERE user_id = #{userId} AND product_id = #{productId}")
    int restoreAndUpdateQuantity(@Param("userId") Long userId, @Param("productId") Long productId, @Param("quantity") Integer quantity);
}
