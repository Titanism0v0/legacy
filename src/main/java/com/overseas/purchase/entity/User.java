package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author System
 */
@Data
@TableName("user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String nickname;
    
    private String avatar;
    
    private String email;
    
    private String phone;
    
    private String role; // USER-普通用户, SELLER-卖家, ADMIN-管理员

    private String kycStatus; // KYC状态：PENDING/APPROVED/REJECTED

    private String kycFiles; // KYC资料文件URL列表（JSON数组字符串）

    private String kycRemark; // KYC审核备注
    
    private String country; // 用户注册国家/地区（对应货币代码）

    private Integer status; // 0-禁用，1-启用
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted; // 0-未删除，1-已删除
}
