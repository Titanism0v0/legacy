import ElementUI from 'element-ui'

// 获取原始的 Message 组件，避免循环引用
const ElMessage = ElementUI.Message

// 封装 Message 组件，设置默认配置
const Message = function(options) {
  if (typeof options === 'string') {
    options = {
      message: options
    }
  }
  options = options || {}
  // 设置默认持续时间为 1.5s，且显示关闭按钮
  options.duration = 1500
  options.showClose = true
  return ElMessage(options)
}

// 挂载便捷方法
;['success', 'warning', 'info', 'error'].forEach(type => {
  Message[type] = options => {
    if (typeof options === 'string') {
      options = {
        message: options
      }
    }
    options = options || {}
    options.type = type
    options.duration = 1500
    options.showClose = true
    return ElMessage(options)
  }
})

// 复制其他静态方法（如 close, closeAll）
Object.keys(ElMessage).forEach(key => {
  if (!Message[key]) {
    Message[key] = ElMessage[key]
  }
})

export default Message
