import store from '../store'

let socket = null
let messageHandlers = []

function getToken() {
  const token = store.state.token
  if (!token) return null
  // axios 拦截器里是直接设置 Bearer <token>，这里只需要原始token即可
  return token
}

export function connect() {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  const token = getToken()
  if (!token) return

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  let host = window.location.host
  // 前后端分离开发时：前端 8081，后端 8080
  if (host.includes(':8081')) {
    host = host.replace(':8081', ':8080')
  }
  const url = `${protocol}://${host}/api/ws/chat?token=${encodeURIComponent(token)}`

  socket = new WebSocket(url)

  socket.onopen = () => {
    console.log('Chat WebSocket connected')
  }

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      messageHandlers.forEach(fn => fn(data))
    } catch (e) {
      console.error('Invalid chat message', e)
    }
  }

  socket.onclose = () => {
    console.log('Chat WebSocket disconnected')
    socket = null
  }

  socket.onerror = (e) => {
    console.error('Chat WebSocket error', e)
  }
}

export function disconnect() {
  if (socket) {
    socket.close()
    socket = null
  }
}

export function sendChat(toUserId, content) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    console.warn('Chat WebSocket not connected')
    return
  }
  const payload = {
    type: 'CHAT',
    toUserId,
    content
  }
  socket.send(JSON.stringify(payload))
}

export function onMessage(handler) {
  if (typeof handler === 'function') {
    messageHandlers.push(handler)
  }
}

export function offMessage(handler) {
  messageHandlers = messageHandlers.filter(fn => fn !== handler)
}

