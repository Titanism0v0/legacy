package com.overseas.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.dto.ProductDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品Mapper接口
 * 
 * @author System
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    /**
     * 查询商品列表（包含卖家信息），数据库分页
     */
    List<ProductDTO> selectProductList(@Param("categoryId") Long categoryId,
                                       @Param("keyword") String keyword,
                                       @Param("status") String status,
                                       @Param("sellerId") Long sellerId,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    /**
     * 查询符合条件的商品总数（用于分页）
     */
    long selectProductCount(@Param("categoryId") Long categoryId,
                            @Param("keyword") String keyword,
                            @Param("status") String status,
                            @Param("sellerId") Long sellerId);
    
    /**
     * 根据ID查询商品详情（包含卖家信息）
     */
    ProductDTO selectProductById(@Param("id") Long id);

    /**
     * 根据ID查询公开可售商品详情（包含卖家信息）
     */
    ProductDTO selectPublicProductById(@Param("id") Long id);
}
