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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Long, Set<WebSocketSession>> USER_SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            USER_SESSION_MAP.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
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
        if (fromUserId == null) {
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

        ChatSession chatSession = chatService.getOrCreateSession(fromUserId, toUserId);
        chatService.saveMessage(chatSession.getId(), fromUserId, toUserId, content);

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

        session.sendMessage(textMessage);

        sendTextMessage(toUserId, textMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            Set<WebSocketSession> sessions = USER_SESSION_MAP.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    USER_SESSION_MAP.remove(userId);
                }
            }
            log.info("WebSocket disconnected, userId={}", userId);
        }
    }

    public void pushEvent(Long userId, String type, Object payload) {
        if (userId == null || type == null) {
            return;
        }
        try {
            RealtimeEvent event = new RealtimeEvent();
            event.setType(type);
            event.setToUserId(userId);
            event.setPayload(payload);
            event.setSendTime(LocalDateTime.now().toString());
            sendTextMessage(userId, new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            log.warn("Push realtime event failed, userId={}, type={}", userId, type, e);
        }
    }

    private void sendTextMessage(Long userId, TextMessage textMessage) {
        Set<WebSocketSession> sessions = USER_SESSION_MAP.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession targetSession : sessions) {
            if (targetSession == null || !targetSession.isOpen()) {
                sessions.remove(targetSession);
                continue;
            }
            try {
                targetSession.sendMessage(textMessage);
            } catch (IOException e) {
                sessions.remove(targetSession);
                log.warn("WebSocket send failed, userId={}", userId, e);
            }
        }
        if (sessions.isEmpty()) {
            USER_SESSION_MAP.remove(userId);
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

    public static class RealtimeEvent {
        private String type;
        private Long toUserId;
        private Object payload;
        private String sendTime;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getToUserId() {
            return toUserId;
        }

        public void setToUserId(Long toUserId) {
            this.toUserId = toUserId;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        public String getSendTime() {
            return sendTime;
        }

        public void setSendTime(String sendTime) {
            this.sendTime = sendTime;
        }
    }
}
