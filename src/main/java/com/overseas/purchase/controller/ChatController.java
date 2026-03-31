package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.ChatSessionDTO;
import com.overseas.purchase.entity.ChatMessage;
import com.overseas.purchase.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/sessions")
    public Result<Page<ChatSessionDTO>> listSessions(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.listMySessions(userId, page, size));
    }

    @GetMapping("/messages")
    public Result<Page<ChatMessage>> listMessages(@RequestParam Long sessionId,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.listMessages(sessionId, page, size, userId));
    }

    @PostMapping("/mark-read")
    public Result<Void> markRead(@RequestParam Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatService.markSessionRead(sessionId, userId);
        return Result.success();
    }

    @PostMapping("/start")
    public Result<ChatSessionDTO> startSession(@RequestParam(required = false) Long peerUserId,
                                               @RequestParam(required = false) Long sellerId,
                                               @RequestParam(required = false) Long buyerId,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("Please login first");
        }

        Long targetUserId = peerUserId != null ? peerUserId : (sellerId != null ? sellerId : buyerId);
        if (targetUserId == null) {
            return Result.error("Target user is required");
        }

        chatService.getOrCreateSession(userId, targetUserId);
        Page<ChatSessionDTO> page = chatService.listMySessions(userId, 1, 100);
        for (ChatSessionDTO session : page.getRecords()) {
            if (targetUserId.equals(session.getPeerUserId())) {
                return Result.success(session);
            }
        }
        return Result.error("Failed to create session");
    }
}
