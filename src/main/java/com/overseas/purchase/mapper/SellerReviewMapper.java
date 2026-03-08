package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.dto.SellerReviewDTO;
import com.overseas.purchase.entity.SellerReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家评价 Mapper
 */
@Mapper
public interface SellerReviewMapper extends BaseMapper<SellerReview> {

    /**
     * 查询商家的评价列表（含评价人信息）
     */
    List<SellerReviewDTO> selectBySellerId(@Param("sellerId") Long sellerId);
}
