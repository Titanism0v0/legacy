package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家评价实体类
 */
@Data
@TableName("seller_review")
public class SellerReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long buyerId;

    private Long sellerId;

    /** 评价星级 1-5 */
    private Integer rating;

    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
