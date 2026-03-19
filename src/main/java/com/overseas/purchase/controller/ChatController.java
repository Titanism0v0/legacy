package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.ChatMessage;
import com.overseas.purchase.entity.ChatSession;
import com.overseas.purchase.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 聊天相关接口（HTTP）
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 我的会话列表
     */
    @GetMapping("/sessions")
    public Result<Page<ChatSession>> listSessions(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        Page<ChatSession> result = chatService.listMySessions(userId, role, page, size);
        return Result.success(result);
    }

    /**
     * 会话消息列表（同时重置当前用户未读）
     */
    @GetMapping("/messages")
    public Result<Page<ChatMessage>> listMessages(@RequestParam Long sessionId,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Page<ChatMessage> result = chatService.listMessages(sessionId, page, size, userId);
        return Result.success(result);
    }

    /**
     * 手动标记会话为已读
     */
    @PostMapping("/mark-read")
    public Result<Void> markRead(@RequestParam Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatService.markSessionRead(sessionId, userId);
        return Result.success();
    }

    /**
     * 按当前用户与指定卖家获取或创建会话
     * 场景：从卖家详情页点击“联系卖家”首次进入聊天页时自动创建会话
     */
    @PostMapping("/start")
    public Result<ChatSession> startSession(@RequestParam Long sellerId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (userId == null || role == null) {
            return Result.error("未登录");
        }
        Long buyerId;
        Long realSellerId;
        if ("USER".equals(role)) {
            buyerId = userId;
            realSellerId = sellerId;
        } else if ("SELLER".equals(role)) {
            buyerId = sellerId;
            realSellerId = userId;
        } else {
            return Result.error("当前角色不支持发起聊天");
        }
        ChatSession session = chatService.getOrCreateSession(buyerId, realSellerId);
        return Result.success(session);
    }
}

