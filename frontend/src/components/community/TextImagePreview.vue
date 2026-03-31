<template>
  <div class="text-image-preview" :style="previewStyle">
    <div class="preview-overlay" />
    <div class="preview-inner" :class="[`align-${template.align || 'left'}`]">
      <div class="preview-head">
        <span class="preview-badge">{{ postTypeLabel || '社区帖子' }}</span>
        <span v-if="primaryEmoji" class="preview-emoji">{{ primaryEmoji }}</span>
      </div>

      <div class="preview-copy">
        <p v-if="subtitleText" class="preview-subtitle">{{ subtitleText }}</p>
        <h3>{{ titleText }}</h3>
        <div class="preview-paragraphs">
          <p v-for="(paragraph, index) in paragraphs" :key="`${paragraph}-${index}`">{{ paragraph }}</p>
        </div>
      </div>

      <div v-if="imageUrls.length" class="preview-gallery" :class="`gallery-${Math.min(imageUrls.length, 3)}`">
        <img v-for="(item, index) in imageUrls.slice(0, 3)" :key="`${item}-${index}`" :src="item" alt="preview">
      </div>

      <div v-if="chips.length" class="preview-tags">
        <span v-for="chip in chips" :key="chip">#{{ chip }}</span>
      </div>

      <div v-if="highlights.length" class="preview-highlights">
        <strong>重点</strong>
        <span v-for="item in highlights" :key="item">{{ item }}</span>
      </div>

      <div class="preview-footer">
        <span>{{ categoryName || '未选择分类' }}</span>
        <span>{{ backgroundLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getTextImageTemplate } from './textImageTemplates'

export default {
  name: 'TextImagePreview',
  props: {
    payload: {
      type: Object,
      default: null
    },
    postTypeLabel: {
      type: String,
      default: ''
    },
    categoryName: {
      type: String,
      default: ''
    }
  },
  computed: {
    templateId() {
      return this.payload && this.payload.render && this.payload.render.templateId
        ? this.payload.render.templateId
        : 'paper'
    },
    template() {
      return getTextImageTemplate(this.templateId)
    },
    previewStyle() {
      const backgroundImage = this.payload && this.payload.render && this.payload.render.backgroundImage
      const background = backgroundImage
        ? `linear-gradient(180deg, rgba(15, 23, 42, 0.18), rgba(15, 23, 42, 0.42)), url(${backgroundImage})`
        : this.template.preview
      return {
        backgroundImage: background,
        color: this.template.textColor,
        borderRadius: `${this.template.radius}px`,
        boxShadow: this.template.shadow
      }
    },
    layout() {
      return (this.payload && this.payload.layout) || {}
    },
    analysis() {
      return (this.payload && this.payload.analysis) || {}
    },
    source() {
      return (this.payload && this.payload.source) || {}
    },
    titleText() {
      return this.layout.displayTitle || this.source.rawTitle || '标题会显示在这里'
    },
    subtitleText() {
      return this.layout.displaySubtitle || ''
    },
    paragraphs() {
      const values = Array.isArray(this.layout.paragraphs) ? this.layout.paragraphs.filter(Boolean) : []
      return values.length ? values : ['点击智能美化后，这里会展示系统自动整理后的分享文案。']
    },
    chips() {
      return Array.isArray(this.layout.chips) ? this.layout.chips.filter(Boolean) : []
    },
    highlights() {
      return Array.isArray(this.layout.highlights) ? this.layout.highlights.filter(Boolean) : []
    },
    imageUrls() {
      return Array.isArray(this.source.sourceImageUrls) ? this.source.sourceImageUrls.filter(Boolean) : []
    },
    primaryEmoji() {
      const values = Array.isArray(this.analysis.recommendedEmoji) ? this.analysis.recommendedEmoji.filter(Boolean) : []
      return values[0] || ''
    },
    backgroundLabel() {
      return this.analysis.recommendedBackgroundTag || '智能背景'
    }
  }
}
</script>

<style scoped>
.text-image-preview {
  position: relative;
  min-height: 560px;
  padding: 28px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.06), rgba(15, 23, 42, 0.28));
}

.preview-inner {
  position: relative;
  z-index: 1;
  min-height: 504px;
  display: flex;
  flex-direction: column;
}

.align-center {
  text-align: center;
}

.preview-head,
.preview-footer,
.preview-highlights {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-badge {
  display: inline-flex;
  align-self: flex-start;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
  font-size: 13px;
  font-weight: 600;
}

.preview-emoji {
  font-size: 28px;
  line-height: 1;
}

.preview-copy {
  margin-top: 30px;
}

.preview-subtitle {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.08em;
  opacity: 0.82;
  text-transform: uppercase;
}

.preview-copy h3 {
  margin: 0;
  font-size: 42px;
  line-height: 1.2;
}

.preview-paragraphs {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.preview-paragraphs p {
  margin: 0;
  font-size: 21px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-gallery {
  display: grid;
  gap: 12px;
  margin-top: 28px;
}

.gallery-1 {
  grid-template-columns: 1fr;
}

.gallery-2 {
  grid-template-columns: repeat(2, 1fr);
}

.gallery-3 {
  grid-template-columns: 1.25fr 0.9fr 0.9fr;
}

.preview-gallery img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 22px;
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.14);
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.preview-tags span,
.preview-highlights span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(8px);
  font-size: 13px;
  font-weight: 600;
}

.preview-highlights {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin-top: 18px;
}

.preview-highlights strong {
  font-size: 13px;
  opacity: 0.86;
}

.preview-footer {
  margin-top: auto;
  padding-top: 26px;
  font-size: 13px;
  opacity: 0.9;
}
</style>
