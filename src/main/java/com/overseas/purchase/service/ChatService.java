package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.entity.ChatMessage;
import com.overseas.purchase.entity.ChatSession;
import com.overseas.purchase.mapper.ChatMessageMapper;
import com.overseas.purchase.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 聊天相关服务
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 按买家/卖家获取或创建会话
     */
    @Transactional
    public ChatSession getOrCreateSession(Long buyerId, Long sellerId) {
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getBuyerId, buyerId)
                        .eq(ChatSession::getSellerId, sellerId)
                        .last("LIMIT 1")
        );
        if (session != null) {
            return session;
        }

        ChatSession newSession = new ChatSession();
        newSession.setBuyerId(buyerId);
        newSession.setSellerId(sellerId);
        newSession.setUnreadForBuyer(0);
        newSession.setUnreadForSeller(0);
        LocalDateTime now = LocalDateTime.now();
        newSession.setCreateTime(now);
        newSession.setUpdateTime(now);
        chatSessionMapper.insert(newSession);
        return newSession;
    }

    /**
     * 保存消息并更新会话摘要/未读数
     */
    @Transactional
    public void saveMessage(Long sessionId, Long fromUserId, Long toUserId, String content) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setContentType("TEXT");
        message.setSendTime(LocalDateTime.now());
        message.setReadFlag(0);
        message.setDeleted(0);
        chatMessageMapper.insert(message);

        // 更新会话摘要与未读数
        ChatSession update = new ChatSession();
        update.setId(session.getId());
        update.setLastMessage(content.length() > 200 ? content.substring(0, 200) : content);
        update.setLastTime(message.getSendTime());

        Integer unreadForBuyer = session.getUnreadForBuyer() == null ? 0 : session.getUnreadForBuyer();
        Integer unreadForSeller = session.getUnreadForSeller() == null ? 0 : session.getUnreadForSeller();
        if (toUserId != null) {
            if (toUserId.equals(session.getBuyerId())) {
                update.setUnreadForBuyer(unreadForBuyer + 1);
                update.setUnreadForSeller(unreadForSeller);
            } else if (toUserId.equals(session.getSellerId())) {
                update.setUnreadForSeller(unreadForSeller + 1);
                update.setUnreadForBuyer(unreadForBuyer);
            }
        }
        update.setUpdateTime(message.getSendTime());
        chatSessionMapper.updateById(update);
    }

    /**
     * 查询当前用户的会话列表
     */
    public Page<ChatSession> listMySessions(Long userId, String role, Integer page, Integer size) {
        Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        if ("USER".equals(role)) {
            wrapper.eq(ChatSession::getBuyerId, userId);
        } else if ("SELLER".equals(role)) {
            wrapper.eq(ChatSession::getSellerId, userId);
        } else {
            // 其他角色（如管理员）暂不返回任何会话
            wrapper.eq(ChatSession::getBuyerId, -1L);
        }
        wrapper.orderByDesc(ChatSession::getLastTime).orderByDesc(ChatSession::getId);
        return chatSessionMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 查询会话消息，并清空当前用户的未读与消息已读标记
     */
    @Transactional
    public Page<ChatMessage> listMessages(Long sessionId, Integer page, Integer size, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!userId.equals(session.getBuyerId()) && !userId.equals(session.getSellerId())) {
            throw new RuntimeException("无权查看该会话");
        }

        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getSendTime)
                .orderByAsc(ChatMessage::getId);
        Page<ChatMessage> result = chatMessageMapper.selectPage(pageParam, wrapper);

        // 将发给当前用户的消息标记为已读
        chatMessageMapper.update(
                null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getToUserId, userId)
                        .eq(ChatMessage::getReadFlag, 0)
                        .set(ChatMessage::getReadFlag, 1)
        );

        // 重置会话未读数
        ChatSession update = new ChatSession();
        update.setId(session.getId());
        if (userId.equals(session.getBuyerId())) {
            update.setUnreadForBuyer(0);
        } else if (userId.equals(session.getSellerId())) {
            update.setUnreadForSeller(0);
        }
        update.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);

        return result;
    }

    /**
     * 手动将会话标记为已读
     */
    @Transactional
    public void markSessionRead(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!userId.equals(session.getBuyerId()) && !userId.equals(session.getSellerId())) {
            throw new RuntimeException("无权操作该会话");
        }

        chatMessageMapper.update(
                null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getToUserId, userId)
                        .set(ChatMessage::getReadFlag, 1)
        );

        ChatSession update = new ChatSession();
        update.setId(session.getId());
        if (userId.equals(session.getBuyerId())) {
            update.setUnreadForBuyer(0);
        } else if (userId.equals(session.getSellerId())) {
            update.setUnreadForSeller(0);
        }
        update.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);
    }
}

