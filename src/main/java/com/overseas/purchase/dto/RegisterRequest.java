package com.overseas.purchase.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Register request payload.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Nickname is required")
    private String nickname;

    private String email;

    private String phone;

    private String role;

    private String country;

    private Boolean agreeTerms;

    private Boolean agreePrivacy;

    private String termsVersion;

    private String privacyVersion;
}
