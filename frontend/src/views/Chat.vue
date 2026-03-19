<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-sessions">
        <div class="sessions-header">
          <h3>会话列表</h3>
        </div>
        <el-scrollbar class="sessions-list">
          <div
            v-for="s in sessions"
            :key="s.id"
            :class="['session-item', { active: s.id === currentSessionId }]"
            @click="selectSession(s)"
          >
            <div class="session-main">
              <div class="session-title">
                <span>{{ getSessionTitle(s) }}</span>
              </div>
              <div class="session-last">
                {{ s.lastMessage || '暂无消息' }}
              </div>
            </div>
            <div class="session-meta">
              <div class="session-time">
                {{ formatTime(s.lastTime) }}
              </div>
              <el-badge
                v-if="getUnread(s) > 0"
                :value="getUnread(s)"
                class="unread-badge"
              />
            </div>
          </div>
          <div v-if="!sessions.length" class="empty-hint">
            暂无会话，去订单或卖家页发起聊天吧
          </div>
        </el-scrollbar>
      </div>

      <div class="chat-messages">
        <div class="messages-header">
          <h3 v-if="currentSessionTitle">{{ currentSessionTitle }}</h3>
          <h3 v-else>选择一个会话开始聊天</h3>
        </div>
        <div class="messages-body">
          <el-scrollbar class="messages-scroll" ref="scrollRef">
            <div
              v-for="m in messages"
              :key="m.id"
              :class="['message-item', { mine: m.fromUserId === currentUserId }]"
            >
              <div class="message-content">
                {{ m.content }}
              </div>
              <div class="message-time">
                {{ formatTime(m.sendTime) }}
              </div>
            </div>
          </el-scrollbar>
        </div>
        <div class="messages-input" v-if="currentSessionId">
          <el-input
            type="textarea"
            v-model="inputText"
            :rows="3"
            placeholder="输入消息，回车发送（Shift+Enter换行）"
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

export default {
  name: 'Chat',
  data() {
    return {
      sessions: [],
      currentSessionId: null,
      currentSessionTitle: '',
      messages: [],
      size: 50,
      inputText: '',
      currentUserId: null,
      targetUserId: null
    }
  },
  created() {
    const user = this.$store.state.user
    this.currentUserId = user && user.id
    connect()
    onMessage(this.handleSocketMessage)
    this.loadSessions().then(() => {
      const sellerId = this.$route.query.sellerId
      if (sellerId) {
        const target = this.sessions.find(
          s => String(s.sellerId) === String(sellerId) || String(s.buyerId) === String(sellerId)
        )
        if (target) {
          this.selectSession(target)
        } else {
          this.startSessionWithSeller(sellerId)
        }
      } else if (this.sessions.length > 0) {
        this.selectSession(this.sessions[0])
      }
    })
  },
  beforeDestroy() {
    offMessage(this.handleSocketMessage)
  },
  methods: {
    async loadSessions() {
      try {
        const res = await chatApi.getSessions({ page: 1, size: 100 })
        const data = res.data || res
        this.sessions = data.records || []
      } catch (e) {
        this.$message.error('加载会话失败')
      }
    },
    async startSessionWithSeller(sellerId) {
      try {
        const res = await chatApi.startSessionWithSeller(sellerId)
        const data = res.data || res
        if (data && data.id) {
          const exists = this.sessions.find(s => s.id === data.id)
          if (!exists) {
            this.sessions.unshift(data)
          }
          this.selectSession(data)
        }
      } catch (e) {
        this.$message.error(e && e.message ? e.message : '创建会话失败')
      }
    },
    async loadMessages(sessionId) {
      try {
        const res = await chatApi.getMessages({ sessionId, page: 1, size: this.size })
        const data = res.data || res
        this.messages = data.records || []
        this.$nextTick(() => {
          const scroll = this.$refs.scrollRef
          if (scroll && scroll.$el) {
            const wrap = scroll.$el.querySelector('.el-scrollbar__wrap')
            if (wrap) {
              wrap.scrollTop = wrap.scrollHeight
            }
          }
        })
      } catch (e) {
        this.$message.error('加载消息失败')
      }
    },
    selectSession(session) {
      this.currentSessionId = session.id
      this.currentSessionTitle = this.getSessionTitle(session)
      if (this.currentUserId === session.buyerId) {
        this.targetUserId = session.sellerId
      } else if (this.currentUserId === session.sellerId) {
        this.targetUserId = session.buyerId
      } else {
        this.targetUserId = null
      }
      this.loadMessages(session.id)
    },
    getSessionTitle(s) {
      const me = this.$store.state.user
      const isBuyer = me && me.id === s.buyerId
      if (isBuyer) {
        return `与卖家 #${s.sellerId} 的会话`
      }
      const isSeller = me && me.id === s.sellerId
      if (isSeller) {
        return `与买家 #${s.buyerId} 的会话`
      }
      return `会话 #${s.id}`
    },
    getUnread(s) {
      const me = this.$store.state.user
      if (!me) return 0
      if (me.id === s.buyerId) return s.unreadForBuyer || 0
      if (me.id === s.sellerId) return s.unreadForSeller || 0
      return 0
    },
    formatTime(t) {
      if (!t) return ''
      const d = typeof t === 'string' ? new Date(t) : t
      if (Number.isNaN(d.getTime())) return ''
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      return `${h}:${m}`
    },
    handleKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
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
    async handleSocketMessage(msg) {
      if (msg.type !== 'CHAT') return
      if (msg.sessionId === this.currentSessionId) {
        this.messages.push({
          id: Date.now(),
          sessionId: msg.sessionId,
          fromUserId: msg.fromUserId,
          toUserId: msg.toUserId,
          content: msg.content,
          sendTime: msg.sendTime
        })
        this.$nextTick(() => {
          const scroll = this.$refs.scrollRef
          if (scroll && scroll.$el) {
            const wrap = scroll.$el.querySelector('.el-scrollbar__wrap')
            if (wrap) {
              wrap.scrollTop = wrap.scrollHeight
            }
          }
        })
      }
      await this.loadSessions()
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
  height: 600px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--card-bg-color);
}

.chat-sessions {
  width: 280px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sessions-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.sessions-header h3 {
  margin: 0;
  font-size: 16px;
}

.sessions-list {
  flex: 1;
}

.session-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color);
}

.session-item.active {
  background-color: var(--primary-color-soft);
}

.session-main {
  flex: 1;
  margin-right: 8px;
}

.session-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.session-last {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}

.session-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.unread-badge {
  margin-top: 4px;
}

.empty-hint {
  padding: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.chat-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.messages-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.messages-header h3 {
  margin: 0;
  font-size: 16px;
}

.messages-body {
  flex: 1;
}

.messages-scroll {
  height: 100%;
  padding: 12px 16px;
}

.message-item {
  max-width: 60%;
  margin-bottom: 10px;
  padding: 6px 10px;
  border-radius: 6px;
  background-color: var(--bg-color);
}

.message-item.mine {
  margin-left: auto;
  background-color: var(--primary-color-soft);
}

.message-content {
  font-size: 14px;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
}

.messages-input {
  border-top: 1px solid var(--border-color);
  padding: 8px 12px;
}

.input-actions {
  margin-top: 4px;
  text-align: right;
}
</style>

