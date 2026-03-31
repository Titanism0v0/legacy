package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.dto.AdminDashboardStatusDTO;
import com.overseas.purchase.dto.AdminDashboardSummaryDTO;
import com.overseas.purchase.dto.AdminDashboardTrendPointDTO;
import com.overseas.purchase.dto.OrderDTO;
import com.overseas.purchase.dto.SellerDashboardStatusDTO;
import com.overseas.purchase.dto.SellerDashboardSummaryDTO;
import com.overseas.purchase.dto.SellerDashboardTrendPointDTO;
import com.overseas.purchase.dto.SellerDashboardTopProductDTO;
import com.overseas.purchase.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    AdminDashboardSummaryDTO selectAdminDashboardSummary(@Param("startTime") LocalDateTime startTime);

    List<AdminDashboardStatusDTO> selectAdminDashboardStatusBreakdown(@Param("startTime") LocalDateTime startTime);

    List<AdminDashboardTrendPointDTO> selectAdminDashboardDailyTrend(@Param("startTime") LocalDateTime startTime);

    SellerDashboardSummaryDTO selectSellerDashboardSummary(@Param("sellerId") Long sellerId,
                                                           @Param("startTime") LocalDateTime startTime);

    List<SellerDashboardStatusDTO> selectSellerDashboardStatusBreakdown(@Param("sellerId") Long sellerId,
                                                                        @Param("startTime") LocalDateTime startTime);

    List<SellerDashboardTrendPointDTO> selectSellerDashboardDailyTrend(@Param("sellerId") Long sellerId,
                                                                       @Param("startTime") LocalDateTime startTime);

    List<SellerDashboardTopProductDTO> selectSellerDashboardTopProducts(@Param("sellerId") Long sellerId,
                                                                        @Param("startTime") LocalDateTime startTime,
                                                                        @Param("limit") Integer limit);

    List<OrderDTO> selectSellerDashboardRecentOrders(@Param("sellerId") Long sellerId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("limit") Integer limit);
}
