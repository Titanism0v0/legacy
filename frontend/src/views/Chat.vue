<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sessions">
        <div class="sessions-header">
          <h3>私信会话</h3>
        </div>
        <el-scrollbar class="sessions-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            :class="['session-item', { active: session.id === currentSessionId }]"
            @click="selectSession(session)"
          >
            <div class="session-main">
              <div class="session-title">
                <Avatar :src="session.peerAvatar" :name="session.peerNickname" :size="28" />
                <span>{{ session.peerNickname || `用户 #${session.peerUserId}` }}</span>
              </div>
              <div class="session-last">{{ session.lastMessage || '暂无消息' }}</div>
            </div>
            <div class="session-meta">
              <div class="session-time">{{ formatTime(session.lastTime) }}</div>
              <el-badge v-if="session.unreadCount > 0" :value="session.unreadCount" class="unread-badge" />
            </div>
          </div>
          <div v-if="!sessions.length" class="empty-hint">
            还没有私信会话，可以从社区帖子、订单或卖家页发起联系。
          </div>
        </el-scrollbar>
      </div>

      <div class="chat-messages">
        <div class="messages-header">
          <template v-if="currentSession">
            <div class="peer-header">
              <Avatar :src="currentSession.peerAvatar" :name="currentSession.peerNickname" :size="36" />
              <div>
                <h3>{{ currentSession.peerNickname || `用户 #${currentSession.peerUserId}` }}</h3>
                <span>点击左侧切换会话</span>
              </div>
            </div>
          </template>
          <h3 v-else>选择一个会话开始聊天</h3>
        </div>
        <div class="messages-body">
          <el-scrollbar class="messages-scroll" ref="scrollRef">
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message-item', { mine: Number(message.fromUserId) === Number(currentUserId) }]"
            >
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatTime(message.sendTime) }}</div>
            </div>
          </el-scrollbar>
        </div>
        <div class="messages-input" v-if="currentSessionId">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入消息，按 Enter 发送，Shift + Enter 换行"
            @keydown.native="handleKeydown"
          />
          <div class="input-actions">
            <el-button type="primary" size="small" @click="send">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { chatApi } from '../api'
import { connect, sendChat, onMessage, offMessage } from '../utils/chatSocket'
import Avatar from '@/components/Avatar.vue'

export default {
  name: 'Chat',
  components: { Avatar },
  data() {
    return {
      sessions: [],
      currentSessionId: null,
      currentSession: null,
      messages: [],
      inputText: '',
      size: 50,
      currentUserId: null,
      targetUserId: null
    }
  },
  created() {
    const user = this.$store.state.user
    this.currentUserId = user && user.id
    connect()
    onMessage(this.handleSocketMessage)
    this.bootstrap()
  },
  beforeDestroy() {
    offMessage(this.handleSocketMessage)
  },
  methods: {
    async bootstrap() {
      await this.loadSessions()
      const peerUserId = this.$route.query.peerUserId || this.$route.query.sellerId || this.$route.query.buyerId
      if (peerUserId) {
        const target = this.sessions.find(session => String(session.peerUserId) === String(peerUserId))
        if (target) {
          this.selectSession(target)
        } else {
          await this.startSession(peerUserId)
        }
      } else if (this.sessions.length > 0) {
        this.selectSession(this.sessions[0])
      }
    },
    async loadSessions() {
      try {
        const res = await chatApi.getSessions({ page: 1, size: 100 })
        const data = res.data || res
        this.sessions = data.records || []
        this.$store.dispatch('refreshChatUnread').catch(() => {})
      } catch (e) {
        this.$message.error('加载会话失败')
      }
    },
    async startSession(peerUserId) {
      try {
        const res = await chatApi.startSession(peerUserId)
        const session = res.data || res
        if (session && session.id) {
          const exists = this.sessions.find(item => item.id === session.id)
          if (!exists) {
            this.sessions.unshift(session)
          }
          this.selectSession(session)
        }
      } catch (e) {
        this.$message.error(e.message || '创建会话失败')
      }
    },
    async loadMessages(sessionId) {
      try {
        const res = await chatApi.getMessages({ sessionId, page: 1, size: this.size })
        const data = res.data || res
        this.messages = data.records || []
        this.scrollToBottom()
        this.$store.dispatch('refreshChatUnread').catch(() => {})
      } catch (e) {
        this.$message.error('加载消息失败')
      }
    },
    selectSession(session) {
      this.currentSessionId = session.id
      this.currentSession = session
      this.targetUserId = session.peerUserId
      this.loadMessages(session.id)
    },
    formatTime(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    },
    handleKeydown(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.send()
      }
    },
    send() {
      const content = (this.inputText || '').trim()
      if (!content) return
      if (!this.targetUserId) {
        this.$message.warning('未找到聊天对象')
        return
      }
      sendChat(this.targetUserId, content)
      this.inputText = ''
    },
    async handleSocketMessage(message) {
      if (!message || message.type !== 'CHAT') return
      if (Number(message.sessionId) === Number(this.currentSessionId)) {
        this.messages.push({
          id: Date.now() + Math.random(),
          sessionId: message.sessionId,
          fromUserId: message.fromUserId,
          toUserId: message.toUserId,
          content: message.content,
          sendTime: message.sendTime
        })
        this.scrollToBottom()
      }
      await this.loadSessions()
      if (this.currentSessionId) {
        const latest = this.sessions.find(session => Number(session.id) === Number(this.currentSessionId))
        if (latest) {
          this.currentSession = latest
        }
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const scroll = this.$refs.scrollRef
        if (!scroll || !scroll.$el) return
        const wrap = scroll.$el.querySelector('.el-scrollbar__wrap')
        if (wrap) {
          wrap.scrollTop = wrap.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.chat-page {
  padding: 20px;
}

.chat-container {
  display: flex;
  min-height: 640px;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  overflow: hidden;
  background: var(--card-bg-color);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.08);
}

.chat-sessions {
  width: 320px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(39, 174, 96, 0.08), transparent 42%);
}

.sessions-header,
.messages-header {
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-color);
}

.sessions-header h3,
.messages-header h3 {
  margin: 0;
  font-size: 18px;
}

.sessions-list {
  flex: 1;
}

.session-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid rgba(127, 140, 141, 0.14);
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.session-item:hover,
.session-item.active {
  background: rgba(39, 174, 96, 0.1);
}

.session-main {
  flex: 1;
  min-width: 0;
}

.session-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  margin-bottom: 6px;
}

.session-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-last {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}

.session-time,
.message-time,
.peer-header span,
.empty-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.chat-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.peer-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.peer-header h3 {
  margin-bottom: 4px;
}

.messages-body {
  flex: 1;
  background:
    radial-gradient(circle at top right, rgba(39, 174, 96, 0.08), transparent 28%),
    radial-gradient(circle at bottom left, rgba(241, 196, 15, 0.08), transparent 24%);
}

.messages-scroll {
  height: 100%;
  padding: 18px;
}

.message-item {
  max-width: 66%;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 14px 14px 14px 4px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(127, 140, 141, 0.15);
}

.message-item.mine {
  margin-left: auto;
  background: rgba(39, 174, 96, 0.16);
  border-radius: 14px 14px 4px 14px;
}

.message-content {
  line-height: 1.6;
  word-break: break-word;
  margin-bottom: 6px;
}

.messages-input {
  border-top: 1px solid var(--border-color);
  padding: 14px 16px;
}

.input-actions {
  margin-top: 8px;
  text-align: right;
}

.empty-hint {
  padding: 18px 16px;
}
</style>
