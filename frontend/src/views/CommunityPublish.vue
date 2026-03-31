<template>
  <div class="publish-page">
    <div class="publish-grid">
      <section class="editor-card">
        <div class="section-head">
          <p class="eyebrow">Publish</p>
          <h1>发布一篇社区帖子</h1>
          <p>先输入原始文案，再点击智能美化，系统会自动整理更适合展示和分享的排版结果。</p>
        </div>

        <el-form label-position="top">
          <el-form-item label="帖子类型">
            <el-radio-group v-model="form.postType">
              <el-radio-button v-for="item in allowedPostTypes" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="标题">
            <el-input
              v-model="form.title"
              maxlength="80"
              show-word-limit
              placeholder="标题可为空，智能美化时系统也可以根据正文自动提炼"
            />
          </el-form-item>

          <el-form-item label="正文">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              maxlength="1000"
              show-word-limit
              placeholder="可以先写原始文案，系统会自动断句、提炼重点并推荐视觉风格"
            />
          </el-form-item>

          <el-form-item label="标签（可选）">
            <el-input
              v-model="tagInput"
              maxlength="12"
              placeholder="输入标签后按回车，例如：海淘、拼单、闲置"
              @keyup.enter.native="appendTag"
              @blur="appendTag"
            >
              <el-button slot="append" @click="appendTag">添加</el-button>
            </el-input>
            <div v-if="form.tags.length" class="tag-list">
              <el-tag
                v-for="tag in form.tags"
                :key="tag"
                closable
                size="medium"
                @close="removeTag(tag)"
              >
                #{{ tag }}
              </el-tag>
            </div>
          </el-form-item>

          <el-form-item label="图片（可选，最多 6 张）">
            <el-upload
              action="/api/upload/community"
              list-type="picture-card"
              :headers="uploadHeaders"
              :file-list="imageList"
              :limit="6"
              accept="image/png,image/jpeg"
              :before-upload="beforeImageUpload"
              :on-success="handleUploadSuccess"
              :on-remove="handleRemove"
            >
              <i class="el-icon-plus" />
            </el-upload>
            <div class="hint">图片会参与智能美化分析，也会继续保存在帖子原始内容中。</div>
          </el-form-item>

          <div class="action-row">
            <el-button @click="$router.push('/community')">取消</el-button>
            <el-button :loading="beautifying" @click="handleBeautify">智能美化</el-button>
            <el-button type="primary" :loading="submitting" @click="submitPost">确认发布</el-button>
          </div>
        </el-form>
      </section>

      <aside class="preview-card">
        <div class="section-head">
          <p class="eyebrow">Preview</p>
          <h2>帖子预览</h2>
        </div>

        <template v-if="beautifyResult">
          <div class="summary-card" :class="{ outdated: beautifyOutdated }">
            <div class="summary-row">
              <span>建议标题</span>
              <strong>{{ beautifyResult.suggestedTitle || '未生成' }}</strong>
            </div>
            <div class="summary-row">
              <span>推荐模板</span>
              <strong>{{ templateName(beautifyResult.recommendedTemplateId) }}</strong>
            </div>
            <div class="summary-row">
              <span>背景风格</span>
              <strong>{{ beautifyResult.recommendedBackgroundTag || '默认' }}</strong>
            </div>
            <div class="summary-row">
              <span>推荐 Emoji</span>
              <strong>{{ formatEmoji(beautifyResult.recommendedEmoji) }}</strong>
            </div>
            <div class="summary-row" v-if="beautifyResult.highlights && beautifyResult.highlights.length">
              <span>高亮信息</span>
              <strong>{{ beautifyResult.highlights.join(' / ') }}</strong>
            </div>
            <div v-if="beautifyOutdated" class="summary-warning">
              当前原始内容已变更，请重新点击“智能美化”再发布。
            </div>
          </div>

          <TextImagePreview
            :payload="beautifyResult.renderPayload"
            :post-type-label="currentPostTypeLabel"
            :category-name="currentCategoryName"
          />
        </template>

        <template v-else>
          <div v-if="primaryImage" class="preview-media image-mode">
            <img :src="primaryImage" alt="preview">
          </div>
          <div v-else class="preview-media text-mode">
            <span class="preview-type">{{ currentPostTypeLabel }}</span>
            <h3>{{ form.title || '点击智能美化后，这里会展示建议标题' }}</h3>
            <p>{{ fallbackPreviewText }}</p>
          </div>
          <div class="preview-body">
            <el-tag size="mini">{{ currentPostTypeLabel }}</el-tag>
            <span class="preview-category">{{ currentCategoryName }}</span>
            <h3>{{ form.title || '先写原始文案，再智能美化' }}</h3>
            <p>{{ form.content || '系统会根据原始文案自动拆分层次、提炼重点、推荐模板与表情。' }}</p>
          </div>
        </template>
      </aside>
    </div>
  </div>
</template>

<script>
import axios from '@/utils/axios'
import { categoryApi, communityApi } from '@/api'
import TextImagePreview from '@/components/community/TextImagePreview.vue'
import { TEXT_IMAGE_TEMPLATES, getTextImageTemplate } from '@/components/community/textImageTemplates'

export default {
  name: 'CommunityPublish',
  components: { TextImagePreview },
  data() {
    return {
      categories: [],
      imageList: [],
      tagInput: '',
      beautifying: false,
      submitting: false,
      beautifyResult: null,
      beautifySignature: '',
      form: {
        postType: 'DISCUSSION',
        categoryId: null,
        title: '',
        content: '',
        tags: []
      }
    }
  },
  computed: {
    userRole() {
      return this.$store.getters.userRole
    },
    uploadHeaders() {
      return { Authorization: `Bearer ${this.$store.state.token}` }
    },
    allowedPostTypes() {
      if (this.userRole === 'SELLER') {
        return [
          { value: 'FOR_SALE', label: '出售帖' },
          { value: 'DISCUSSION', label: '讨论帖' }
        ]
      }
      return [
        { value: 'WANTED', label: '求购帖' },
        { value: 'DISCUSSION', label: '讨论帖' }
      ]
    },
    currentPostTypeLabel() {
      const current = this.allowedPostTypes.find(item => item.value === this.form.postType)
      return current ? current.label : '帖子'
    },
    currentCategoryName() {
      const category = this.categories.find(item => Number(item.id) === Number(this.form.categoryId))
      return category ? category.name : '请选择分类'
    },
    primaryImage() {
      const first = this.imageList[0]
      return first ? this.resolveUrlFromFile(first) : ''
    },
    fallbackPreviewText() {
      const text = (this.form.content || '').trim()
      if (!text) {
        return '输入标题、正文、标签和可选图片后，点击“智能美化”查看系统自动整理后的分享图效果。'
      }
      return text.length > 150 ? `${text.slice(0, 150)}...` : text
    },
    beautifyOutdated() {
      return !!this.beautifyResult && this.beautifySignature !== this.buildBeautifySignature()
    }
  },
  created() {
    this.loadCategories()
    if (this.userRole === 'SELLER') {
      this.form.postType = 'FOR_SALE'
    }
  },
  methods: {
    async loadCategories() {
      try {
        const res = await categoryApi.getAllCategories()
        this.categories = res.data || []
      } catch (e) {
        this.categories = []
      }
    },
    beforeImageUpload(file) {
      const isAllowed = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isAllowed) this.$message.error('只支持 JPG/PNG 图片')
      if (!isLt5M) this.$message.error('单张图片不能超过 5MB')
      return isAllowed && isLt5M
    },
    handleUploadSuccess(response, file, fileList) {
      if (response.code !== 200) {
        this.$message.error(response.message || '上传失败')
        return
      }
      this.imageList = fileList
    },
    handleRemove(file, fileList) {
      this.imageList = fileList
    },
    resolveUrlFromFile(file) {
      if (!file) return ''
      return file.url || (file.response && file.response.data && file.response.data.url) || ''
    },
    appendTag() {
      const input = (this.tagInput || '').trim()
      if (!input) return
      const candidates = input
        .split(/[，,\s]+/)
        .map(item => item.trim().replace(/^#/, ''))
        .filter(Boolean)

      for (let i = 0; i < candidates.length; i++) {
        if (this.form.tags.length >= 6) {
          this.$message.warning('标签最多添加 6 个')
          break
        }
        const tag = candidates[i]
        if (!this.form.tags.includes(tag)) {
          this.form.tags.push(tag)
        }
      }
      this.tagInput = ''
    },
    removeTag(tag) {
      this.form.tags = this.form.tags.filter(item => item !== tag)
    },
    getImageUrls() {
      return this.imageList.map(file => this.resolveUrlFromFile(file)).filter(Boolean)
    },
    buildBeautifySignature() {
      return JSON.stringify({
        postType: this.form.postType,
        categoryId: this.form.categoryId,
        title: (this.form.title || '').trim(),
        content: (this.form.content || '').trim(),
        tags: this.form.tags.slice().sort(),
        imageUrls: this.getImageUrls()
      })
    },
    async handleBeautify() {
      this.appendTag()
      if (!this.form.categoryId) {
        this.$message.warning('请先选择分类')
        return
      }
      if (!this.form.title.trim() && !this.form.content.trim()) {
        this.$message.warning('请至少输入标题或正文，再进行智能美化')
        return
      }

      this.beautifying = true
      try {
        const res = await communityApi.beautifyPost({
          postType: this.form.postType,
          categoryId: this.form.categoryId,
          title: this.form.title.trim() || null,
          content: this.form.content.trim() || null,
          tags: this.form.tags,
          imageUrls: this.getImageUrls()
        })
        this.beautifyResult = res.data || res
        this.beautifySignature = this.buildBeautifySignature()
        this.$message.success('智能美化完成')
      } catch (e) {
        this.$message.error(e.message || '智能美化失败')
      } finally {
        this.beautifying = false
      }
    },
    validatePublish() {
      if (!this.form.categoryId) {
        this.$message.warning('请选择分类')
        return false
      }
      if (!this.beautifyResult) {
        if (!this.form.title.trim()) {
          this.$message.warning('请输入标题')
          return false
        }
        if (!this.form.content.trim()) {
          this.$message.warning('未使用智能美化时，正文不能为空')
          return false
        }
      }
      if (this.beautifyOutdated) {
        this.$message.warning('原始内容已经变更，请重新点击智能美化')
        return false
      }
      return true
    },
    async submitPost() {
      this.appendTag()
      if (!this.validatePublish()) {
        return
      }

      this.submitting = true
      try {
        const imageUrls = this.getImageUrls()
        const payload = {
          postType: this.form.postType,
          categoryId: this.form.categoryId,
          title: this.form.title.trim(),
          content: this.form.content.trim(),
          images: imageUrls.length ? JSON.stringify(imageUrls) : null,
          coverImage: null,
          coverTemplate: null,
          renderPayload: null,
          contentMode: 'STANDARD'
        }

        if (this.beautifyResult) {
          const renderPayload = this.beautifyResult.renderPayload
          const coverBlob = await this.generateSmartCoverBlob(renderPayload)
          payload.coverImage = await this.uploadGeneratedCover(coverBlob)
          payload.coverTemplate = renderPayload.render.templateId
          payload.renderPayload = JSON.stringify(renderPayload)
          payload.contentMode = 'TEXT_IMAGE'
          if (!payload.title) {
            payload.title = this.beautifyResult.suggestedTitle || '社区分享'
          }
        } else if (!imageUrls.length) {
          const coverBlob = await this.generateFallbackCoverBlob()
          payload.coverImage = await this.uploadGeneratedCover(coverBlob)
          payload.coverTemplate = 'paper'
        }

        await communityApi.createPost(payload)
        this.$message.success('帖子已发布')
        this.$router.push('/community')
      } catch (e) {
        this.$message.error(e.message || '发布失败')
      } finally {
        this.submitting = false
      }
    },
    templateName(templateId) {
      const template = TEXT_IMAGE_TEMPLATES.find(item => item.id === templateId)
      return template ? template.name : '默认模板'
    },
    formatEmoji(values) {
      return Array.isArray(values) && values.length ? values.join(' ') : '无'
    },
    async generateFallbackCoverBlob() {
      const payload = {
        layout: {
          displayTitle: this.form.title.trim() || '社区分享',
          displaySubtitle: this.currentCategoryName,
          paragraphs: [this.fallbackPreviewText],
          chips: this.form.tags,
          highlights: []
        },
        analysis: {
          recommendedEmoji: [],
          recommendedBackgroundTag: 'default'
        },
        source: {
          sourceImageUrls: []
        },
        render: {
          templateId: 'paper',
          backgroundTag: 'default',
          backgroundImage: null
        }
      }
      return this.generateSmartCoverBlob(payload)
    },
    async generateSmartCoverBlob(renderPayload) {
      const canvas = document.createElement('canvas')
      canvas.width = 1080
      canvas.height = 1440
      const ctx = canvas.getContext('2d')
      const template = getTextImageTemplate(renderPayload.render && renderPayload.render.templateId)
      const backgroundImage = renderPayload.render && renderPayload.render.backgroundImage

      if (backgroundImage) {
        const image = await this.loadImage(backgroundImage)
        ctx.drawImage(image, 0, 0, canvas.width, canvas.height)
        ctx.fillStyle = 'rgba(15, 23, 42, 0.28)'
        ctx.fillRect(0, 0, canvas.width, canvas.height)
      } else {
        const bg = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
        bg.addColorStop(0, template.accent[0])
        bg.addColorStop(1, template.accent[1])
        ctx.fillStyle = bg
        ctx.fillRect(0, 0, canvas.width, canvas.height)
      }

      ctx.save()
      ctx.fillStyle = template.id === 'paper' ? 'rgba(255, 250, 241, 0.94)' : 'rgba(255, 255, 255, 0.12)'
      this.drawRoundedRect(ctx, 56, 56, 968, 1328, template.radius || 34)
      ctx.restore()

      const layout = renderPayload.layout || {}
      const analysis = renderPayload.analysis || {}
      const source = renderPayload.source || {}
      const align = template.align === 'center' ? 'center' : 'left'
      const leftX = align === 'center' ? canvas.width / 2 : 112
      const maxWidth = align === 'center' ? 790 : 856
      const textColor = template.textColor || '#ffffff'
      let cursorY = 150

      ctx.textAlign = align
      ctx.fillStyle = template.badgeBackground || 'rgba(255,255,255,0.16)'
      this.drawRoundedRect(ctx, align === 'center' ? 112 : 100, 108, 280, 62, 31)
      ctx.fillStyle = textColor
      ctx.font = '600 30px sans-serif'
      ctx.fillText(this.currentPostTypeLabel, align === 'center' ? 252 : 136, 149)

      const emoji = Array.isArray(analysis.recommendedEmoji) && analysis.recommendedEmoji.length
        ? analysis.recommendedEmoji[0]
        : ''
      if (emoji) {
        ctx.font = '42px sans-serif'
        ctx.fillText(emoji, align === 'center' ? 920 : 940, 154)
      }

      if (layout.displaySubtitle) {
        ctx.font = '600 24px sans-serif'
        ctx.globalAlpha = 0.84
        cursorY = 232
        ctx.fillText(layout.displaySubtitle, leftX, cursorY)
        ctx.globalAlpha = 1
      } else {
        cursorY = 220
      }

      ctx.font = `700 ${template.titleSize || 48}px sans-serif`
      cursorY = this.drawWrappedText(ctx, layout.displayTitle || this.form.title.trim(), leftX, cursorY + 48, maxWidth, 68, 3) + 8

      const paragraphs = Array.isArray(layout.paragraphs) && layout.paragraphs.length
        ? layout.paragraphs
        : [this.form.content.trim() || this.fallbackPreviewText]
      ctx.font = `400 ${template.contentSize || 24}px sans-serif`
      paragraphs.slice(0, 5).forEach((paragraph) => {
        cursorY = this.drawWrappedText(ctx, paragraph, leftX, cursorY + 28, maxWidth, 42, 3) + 8
      })

      const imageUrls = Array.isArray(source.sourceImageUrls) ? source.sourceImageUrls.slice(0, 3) : []
      if (imageUrls.length) {
        const imageY = cursorY + 22
        await this.drawMaterialGallery(ctx, imageUrls, imageY)
        cursorY = imageY + 292
      }

      const chips = Array.isArray(layout.chips) ? layout.chips : []
      if (chips.length) {
        cursorY = this.drawTagList(ctx, chips, cursorY + 20, textColor, template.tagBackground || 'rgba(255,255,255,0.16)', align)
      }

      const highlights = Array.isArray(layout.highlights) ? layout.highlights : []
      if (highlights.length) {
        cursorY = this.drawTagList(ctx, highlights, cursorY + 20, textColor, 'rgba(15, 23, 42, 0.16)', align)
      }

      ctx.font = '500 24px sans-serif'
      ctx.fillStyle = textColor
      ctx.fillText(this.currentCategoryName, align === 'center' ? canvas.width / 2 : 112, 1340)
      ctx.globalAlpha = 0.82
      ctx.fillText((analysis.recommendedBackgroundTag || '智能背景').replace(/-/g, ' '), align === 'center' ? canvas.width / 2 : 112, 1380)
      ctx.globalAlpha = 1

      return new Promise((resolve, reject) => {
        canvas.toBlob((blob) => {
          if (!blob) {
            reject(new Error('生成智能封面失败'))
            return
          }
          resolve(blob)
        }, 'image/jpeg', 0.9)
      })
    },
    async drawMaterialGallery(ctx, urls, startY) {
      const loaded = await Promise.all(urls.map(url => this.loadImage(url)))
      const boxes = [
        { x: 112, y: startY, width: loaded.length === 1 ? 856 : 520, height: 250 },
        { x: 648, y: startY, width: 320, height: 119 },
        { x: 648, y: startY + 131, width: 320, height: 119 }
      ]
      for (let i = 0; i < loaded.length; i++) {
        this.drawCoverImage(ctx, loaded[i], boxes[i].x, boxes[i].y, boxes[i].width, boxes[i].height, 26)
      }
    },
    drawCoverImage(ctx, image, x, y, width, height, radius) {
      ctx.save()
      this.clipRoundedRect(ctx, x, y, width, height, radius)
      const sourceRatio = image.width / image.height
      const targetRatio = width / height
      let drawWidth = width
      let drawHeight = height
      let drawX = x
      let drawY = y

      if (sourceRatio > targetRatio) {
        drawWidth = height * sourceRatio
        drawX = x - (drawWidth - width) / 2
      } else {
        drawHeight = width / sourceRatio
        drawY = y - (drawHeight - height) / 2
      }

      ctx.drawImage(image, drawX, drawY, drawWidth, drawHeight)
      ctx.restore()
    },
    drawTagList(ctx, tags, startY, textColor, background, align) {
      let x = align === 'center' ? 140 : 112
      let y = startY
      const maxWidth = 856
      const originalAlign = ctx.textAlign
      ctx.font = '600 22px sans-serif'
      ctx.textAlign = 'center'
      tags.slice(0, 6).forEach((tag) => {
        const label = `${tag}`
        const chipWidth = Math.min(ctx.measureText(label).width + 36, 240)
        if (x + chipWidth > 112 + maxWidth) {
          x = align === 'center' ? 140 : 112
          y += 54
        }
        ctx.fillStyle = background
        this.drawRoundedRect(ctx, x, y, chipWidth, 38, 19)
        ctx.fillStyle = textColor
        ctx.fillText(label, x + chipWidth / 2, y + 25)
        x += chipWidth + 12
      })
      ctx.textAlign = originalAlign
      return y + 44
    },
    drawWrappedText(ctx, text, x, startY, maxWidth, lineHeight, maxLines) {
      const value = (text || '').trim()
      if (!value) return startY
      const chars = value.split('')
      const lines = []
      let current = ''
      chars.forEach((char) => {
        const next = current + char
        if (ctx.measureText(next).width > maxWidth && current) {
          lines.push(current)
          current = char
        } else {
          current = next
        }
      })
      if (current) lines.push(current)

      const visibleLines = lines.slice(0, maxLines)
      visibleLines.forEach((line, index) => {
        let output = line
        if (index === visibleLines.length - 1 && lines.length > maxLines) {
          output = `${line.replace(/\s+$/, '')}...`
        }
        ctx.fillText(output, x, startY + lineHeight * index)
      })
      return startY + lineHeight * visibleLines.length
    },
    drawRoundedRect(ctx, x, y, width, height, radius) {
      ctx.beginPath()
      ctx.moveTo(x + radius, y)
      ctx.lineTo(x + width - radius, y)
      ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
      ctx.lineTo(x + width, y + height - radius)
      ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
      ctx.lineTo(x + radius, y + height)
      ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
      ctx.lineTo(x, y + radius)
      ctx.quadraticCurveTo(x, y, x + radius, y)
      ctx.closePath()
      ctx.fill()
    },
    clipRoundedRect(ctx, x, y, width, height, radius) {
      ctx.beginPath()
      ctx.moveTo(x + radius, y)
      ctx.lineTo(x + width - radius, y)
      ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
      ctx.lineTo(x + width, y + height - radius)
      ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
      ctx.lineTo(x + radius, y + height)
      ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
      ctx.lineTo(x, y + radius)
      ctx.quadraticCurveTo(x, y, x + radius, y)
      ctx.closePath()
      ctx.clip()
    },
    loadImage(url) {
      return new Promise((resolve, reject) => {
        const image = new Image()
        image.crossOrigin = 'anonymous'
        image.onload = () => resolve(image)
        image.onerror = () => reject(new Error('图片加载失败'))
        image.src = url
      })
    },
    async uploadGeneratedCover(blob) {
      const formData = new FormData()
      formData.append('file', blob, `community-cover-${Date.now()}.jpg`)
      const res = await axios.post('/upload/community', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const data = res.data || res
      return data.url
    }
  }
}
</script>

<style scoped>
.publish-page {
  padding: 8px 0 24px;
}

.publish-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(360px, 0.95fr);
  gap: 22px;
}

.editor-card,
.preview-card {
  background: var(--card-bg-color);
  border-radius: 28px;
  border: 1px solid rgba(127, 140, 141, 0.14);
  box-shadow: 0 16px 35px rgba(0, 0, 0, 0.06);
}

.editor-card {
  padding: 28px;
}

.preview-card {
  padding: 24px;
  position: sticky;
  top: 18px;
  height: fit-content;
}

.section-head {
  margin-bottom: 22px;
}

.section-head h1,
.section-head h2 {
  margin: 6px 0 0;
}

.section-head p:last-child {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: var(--text-secondary);
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.summary-card {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(39, 174, 96, 0.06);
  border: 1px solid rgba(39, 174, 96, 0.12);
}

.summary-card.outdated {
  background: rgba(241, 196, 15, 0.08);
  border-color: rgba(241, 196, 15, 0.22);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.summary-row span {
  color: var(--text-secondary);
}

.summary-row strong {
  text-align: right;
}

.summary-warning {
  font-size: 12px;
  color: #b7791f;
}

.preview-media {
  border-radius: 24px;
  min-height: 360px;
  overflow: hidden;
  margin-bottom: 18px;
  background: linear-gradient(145deg, #f3f4f6, #e5e7eb);
}

.image-mode img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.text-mode {
  padding: 26px;
  color: #22303c;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.preview-type {
  align-self: flex-start;
  margin-bottom: auto;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(34, 48, 60, 0.08);
}

.text-mode h3 {
  margin: 0 0 12px;
  font-size: 32px;
  line-height: 1.25;
}

.text-mode p,
.preview-body p {
  line-height: 1.8;
}

.preview-body {
  display: grid;
  gap: 12px;
}

.preview-category {
  font-size: 13px;
  color: var(--text-secondary);
}

.preview-body h3,
.preview-body p {
  margin: 0;
}

.preview-body p {
  color: var(--text-secondary);
}

@media (max-width: 1100px) {
  .publish-grid {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
  }
}
</style>
