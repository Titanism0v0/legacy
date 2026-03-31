package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.dto.ChatSessionDTO;
import com.overseas.purchase.entity.ChatMessage;
import com.overseas.purchase.entity.ChatSession;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.ChatMessageMapper;
import com.overseas.purchase.mapper.ChatSessionMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;

    @Transactional
    public ChatSession getOrCreateSession(Long leftUserId, Long rightUserId) {
        if (leftUserId == null || rightUserId == null) {
            throw new RuntimeException("Invalid chat participants");
        }
        if (leftUserId.equals(rightUserId)) {
            throw new RuntimeException("Cannot start chat with yourself");
        }

        Long userAId = Math.min(leftUserId, rightUserId);
        Long userBId = Math.max(leftUserId, rightUserId);
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserAId, userAId)
                        .eq(ChatSession::getUserBId, userBId)
                        .last("LIMIT 1")
        );
        if (session != null) {
            return session;
        }

        ChatSession newSession = new ChatSession();
        newSession.setUserAId(userAId);
        newSession.setUserBId(userBId);
        newSession.setUnreadForA(0);
        newSession.setUnreadForB(0);
        LocalDateTime now = LocalDateTime.now();
        newSession.setCreateTime(now);
        newSession.setUpdateTime(now);
        chatSessionMapper.insert(newSession);
        return newSession;
    }

    @Transactional
    public void saveMessage(Long sessionId, Long fromUserId, Long toUserId, String content) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session does not exist");
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

        ChatSession update = new ChatSession();
        update.setId(session.getId());
        update.setLastMessage(content.length() > 200 ? content.substring(0, 200) : content);
        update.setLastTime(message.getSendTime());

        Integer unreadForA = session.getUnreadForA() == null ? 0 : session.getUnreadForA();
        Integer unreadForB = session.getUnreadForB() == null ? 0 : session.getUnreadForB();
        if (toUserId.equals(session.getUserAId())) {
            update.setUnreadForA(unreadForA + 1);
            update.setUnreadForB(unreadForB);
        } else if (toUserId.equals(session.getUserBId())) {
            update.setUnreadForB(unreadForB + 1);
            update.setUnreadForA(unreadForA);
        }
        update.setUpdateTime(message.getSendTime());
        chatSessionMapper.updateById(update);
    }

    public Page<ChatSessionDTO> listMySessions(Long userId, Integer page, Integer size) {
        Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .and(w -> w.eq(ChatSession::getUserAId, userId).or().eq(ChatSession::getUserBId, userId))
                .orderByDesc(ChatSession::getLastTime)
                .orderByDesc(ChatSession::getId);
        Page<ChatSession> sessionPage = chatSessionMapper.selectPage(pageParam, wrapper);

        Set<Long> peerIds = new HashSet<>();
        for (ChatSession session : sessionPage.getRecords()) {
            peerIds.add(resolvePeerUserId(session, userId));
        }
        Map<Long, User> users = loadUsers(peerIds);

        Page<ChatSessionDTO> dtoPage = new Page<>(sessionPage.getCurrent(), sessionPage.getSize(), sessionPage.getTotal());
        dtoPage.setRecords(sessionPage.getRecords().stream()
                .map(session -> toSessionDTO(session, userId, users))
                .collect(Collectors.toList()));
        return dtoPage;
    }

    @Transactional
    public Page<ChatMessage> listMessages(Long sessionId, Integer page, Integer size, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session does not exist");
        }
        ensureSessionMember(session, userId);

        Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getSendTime)
                .orderByAsc(ChatMessage::getId);
        Page<ChatMessage> result = chatMessageMapper.selectPage(pageParam, wrapper);

        chatMessageMapper.update(
                null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getToUserId, userId)
                        .eq(ChatMessage::getReadFlag, 0)
                        .set(ChatMessage::getReadFlag, 1)
        );

        ChatSession update = new ChatSession();
        update.setId(session.getId());
        if (userId.equals(session.getUserAId())) {
            update.setUnreadForA(0);
        } else {
            update.setUnreadForB(0);
        }
        update.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);

        return result;
    }

    @Transactional
    public void markSessionRead(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session does not exist");
        }
        ensureSessionMember(session, userId);

        chatMessageMapper.update(
                null,
                new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getToUserId, userId)
                        .set(ChatMessage::getReadFlag, 1)
        );

        ChatSession update = new ChatSession();
        update.setId(session.getId());
        if (userId.equals(session.getUserAId())) {
            update.setUnreadForA(0);
        } else {
            update.setUnreadForB(0);
        }
        update.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);
    }

    private void ensureSessionMember(ChatSession session, Long userId) {
        if (!userId.equals(session.getUserAId()) && !userId.equals(session.getUserBId())) {
            throw new RuntimeException("No permission to access this session");
        }
    }

    private Long resolvePeerUserId(ChatSession session, Long currentUserId) {
        return currentUserId.equals(session.getUserAId()) ? session.getUserBId() : session.getUserAId();
    }

    private ChatSessionDTO toSessionDTO(ChatSession session, Long currentUserId, Map<Long, User> users) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setLastMessage(session.getLastMessage());
        dto.setLastTime(session.getLastTime());

        Long peerUserId = resolvePeerUserId(session, currentUserId);
        dto.setPeerUserId(peerUserId);
        User peer = users.get(peerUserId);
        if (peer != null) {
            dto.setPeerNickname(peer.getNickname() != null && !peer.getNickname().trim().isEmpty()
                    ? peer.getNickname()
                    : peer.getUsername());
            dto.setPeerAvatar(peer.getAvatar());
        } else {
            dto.setPeerNickname("User #" + peerUserId);
        }

        if (currentUserId.equals(session.getUserAId())) {
            dto.setUnreadCount(session.getUnreadForA() == null ? 0 : session.getUnreadForA());
        } else {
            dto.setUnreadCount(session.getUnreadForB() == null ? 0 : session.getUnreadForB());
        }
        return dto;
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> result = new HashMap<>();
        for (User user : users) {
            if (user != null && (user.getDeleted() == null || user.getDeleted() == 0)) {
                result.put(user.getId(), user);
            }
        }
        return result;
    }
}
