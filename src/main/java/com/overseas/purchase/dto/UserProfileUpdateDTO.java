package com.overseas.purchase.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {

    private Long id;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String country;
}
