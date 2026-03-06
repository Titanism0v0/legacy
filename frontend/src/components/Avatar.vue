// 根据用户名生成头像
<template>
  <div class="avatar-container" :style="containerStyle">
    <img v-if="src" :src="src" class="avatar-img" :style="imgStyle" @error="handleImageError" />
    <span v-else class="avatar-text" :style="textStyle">{{ initials }}</span>
  </div>
</template>

<script>
export default {
  name: 'Avatar',
  props: {
    src: {
      type: String,
      default: ''
    },
    name: {
      type: String,
      default: ''
    },
    size: {
      type: Number,
      default: 40
    },
    backgroundColor: {
      type: String,
      default: ''
    },
    color: {
      type: String,
      default: '#ffffff'
    }
  },
  data() {
    return {
      imageLoadError: false
    }
  },
  computed: {
    initials() {
      if (!this.name) return 'U'
      
      // 如果是中文，取最后一个字
      if (/[\u4e00-\u9fa5]/.test(this.name)) {
        return this.name.slice(-1)
      }
      
      // 如果是英文，取前两个单词的首字母，或者前两个字符
      const parts = this.name.trim().split(/\s+/)
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase()
      }
      return this.name.slice(0, 2).toUpperCase()
    },
    generatedBackgroundColor() {
      if (this.backgroundColor) return this.backgroundColor
      
      // 根据名字生成固定的背景色
      const colors = [
        '#3375E0', // Primary Blue
        '#10B981', // Success Green
        '#F59E0B', // Warning Orange
        '#EF4444', // Danger Red
        '#8B5CF6', // Purple
        '#EC4899', // Pink
        '#6366F1', // Indigo
        '#14B8A6'  // Teal
      ]
      
      let hash = 0
      for (let i = 0; i < this.name.length; i++) {
        hash = this.name.charCodeAt(i) + ((hash << 5) - hash)
      }
      
      const index = Math.abs(hash) % colors.length
      return colors[index]
    },
    containerStyle() {
      return {
        width: `${this.size}px`,
        height: `${this.size}px`,
        borderRadius: '50%',
        backgroundColor: this.src && !this.imageLoadError ? 'transparent' : this.generatedBackgroundColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        userSelect: 'none',
        flexShrink: 0,
        overflow: 'hidden'
      }
    },
    textStyle() {
      return {
        color: this.color,
        fontSize: `${this.size * 0.4}px`,
        fontWeight: 'bold',
        lineHeight: 1
      }
    },
    imgStyle() {
      return {
        width: '100%',
        height: '100%',
        objectFit: 'cover'
      }
    }
  },
  methods: {
    handleImageError() {
      console.error('Avatar load error:', this.src)
      this.imageLoadError = true
    }
  },
  watch: {
    src() {
      this.imageLoadError = false
    }
  }
}
</script>

<style scoped>
.avatar-container {
  display: inline-flex;
  vertical-align: middle;
}
.avatar-img {
  display: block;
}
</style>