package com.overseas.purchase.dto;

import lombok.Data;

/**
 * 用户DTO
 * 
 * @author System
 */
@Data
public class UserDTO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String email;
    
    private String phone;
    
    private String role;

    private String kycStatus;

    private String kycFiles;

    private String kycRemark;
    
    private String country;

    private Integer status;
}
