package com.overseas.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionDTO {

    private Long id;

    private Long peerUserId;

    private String peerNickname;

    private String peerAvatar;

    private String lastMessage;

    private LocalDateTime lastTime;

    private Integer unreadCount;
}
