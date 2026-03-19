package com.overseas.purchase.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overseas.purchase.entity.ChatSession;
import com.overseas.purchase.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 简单按 userId 保存单连接
    private static final Map<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            USER_SESSION_MAP.put(userId, session);
            log.info("WebSocket connected, userId={}", userId);
        } else {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = getUserId(session);
        String role = getRole(session);
        if (fromUserId == null || role == null) {
            log.warn("Chat handleTextMessage missing auth, closing. attrs={}", session.getAttributes());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        JsonNode node = objectMapper.readTree(message.getPayload());
        String type = node.path("type").asText("CHAT");
        if (!"CHAT".equalsIgnoreCase(type)) {
            return;
        }

        Long toUserId = node.path("toUserId").asLong();
        String content = node.path("content").asText("");
        if (toUserId == 0L || content.isEmpty()) {
            log.warn("Chat handleTextMessage invalid payload, toUserId={}, contentLen={}", toUserId, content.length());
            return;
        }

        // 根据角色推断 buyer/seller
        Long buyerId;
        Long sellerId;
        if ("USER".equals(role)) {
            buyerId = fromUserId;
            sellerId = toUserId;
        } else if ("SELLER".equals(role)) {
            buyerId = toUserId;
            sellerId = fromUserId;
        } else {
            // 其他角色暂不支持聊天
            log.warn("Chat handleTextMessage unsupported role={}, userId={}", role, fromUserId);
            return;
        }

        ChatSession chatSession = chatService.getOrCreateSession(buyerId, sellerId);
        chatService.saveMessage(chatSession.getId(), fromUserId, toUserId, content);

        // 构造下行消息
        LocalDateTime now = LocalDateTime.now();
        OutgoingMessage outgoing = new OutgoingMessage();
        outgoing.setType("CHAT");
        outgoing.setSessionId(chatSession.getId());
        outgoing.setFromUserId(fromUserId);
        outgoing.setToUserId(toUserId);
        outgoing.setContent(content);
        outgoing.setSendTime(now.toString());

        String json = objectMapper.writeValueAsString(outgoing);
        TextMessage textMessage = new TextMessage(json);

        // 发给自己
        session.sendMessage(textMessage);

        // 发给对端（如果在线）
        WebSocketSession toSession = USER_SESSION_MAP.get(toUserId);
        if (toSession != null && toSession.isOpen()) {
            toSession.sendMessage(textMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            USER_SESSION_MAP.remove(userId);
            log.info("WebSocket disconnected, userId={}", userId);
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object val = session.getAttributes().get("userId");
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    private String getRole(WebSocketSession session) {
        Object val = session.getAttributes().get("role");
        return val != null ? val.toString() : null;
    }

    // 下行消息结构
    public static class OutgoingMessage {
        private String type;
        private Long sessionId;
        private Long fromUserId;
        private Long toUserId;
        private String content;
        private String sendTime;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getFromUserId() {
            return fromUserId;
        }

        public void setFromUserId(Long fromUserId) {
            this.fromUserId = fromUserId;
        }

        public Long getToUserId() {
            return toUserId;
        }

        public void setToUserId(Long toUserId) {
            this.toUserId = toUserId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSendTime() {
            return sendTime;
        }

        public void setSendTime(String sendTime) {
            this.sendTime = sendTime;
        }
    }
}

