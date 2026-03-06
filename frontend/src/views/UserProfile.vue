<template>
  <div class="user-profile">
    <h2>个人中心</h2>
    
    <div class="profile-container">
      <!-- 左侧：基本信息 -->
      <el-card class="box-card profile-card">
        <div slot="header" class="clearfix">
          <span>基本信息</span>
        </div>
        <div class="user-info">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              action=""
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleFileChange">
              <Avatar v-if="userForm.avatar" :name="userForm.nickname || userForm.username" :src="userForm.avatar" :size="120" class="avatar" />
              <i v-else class="el-icon-plus avatar-uploader-icon"></i>
            </el-upload>
            <div class="avatar-tip">点击上传头像</div>
          </div>
          
          <el-form :model="userForm" label-width="80px" class="info-form">
            <el-form-item label="用户名">
              <span>{{ userForm.username }}</span>
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="userForm.nickname" placeholder="请输入昵称"></el-input>
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="userForm.email" placeholder="请输入邮箱"></el-input>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="userForm.phone" placeholder="请输入手机号"></el-input>
            </el-form-item>
            <el-form-item label="国家/地区">
              <el-select v-model="userForm.country" placeholder="请选择国家/地区" style="width: 100%;">
                <el-option label="中国 (CNY)" value="CNY"></el-option>
                <el-option label="美国 (USD)" value="USD"></el-option>
                <el-option label="日本 (JPY)" value="JPY"></el-option>
                <el-option label="欧洲 (EUR)" value="EUR"></el-option>
                <el-option label="英国 (GBP)" value="GBP"></el-option>
                <el-option label="韩国 (KRW)" value="KRW"></el-option>
                <el-option label="加拿大 (CAD)" value="CAD"></el-option>
                <el-option label="澳大利亚 (AUD)" value="AUD"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="会员等级">
              <el-tag type="warning">普通会员</el-tag>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 右侧：功能入口 -->
      <div class="right-section">
        <!-- 地址管理入口 -->
        <el-card class="box-card action-card" shadow="hover" @click.native="$router.push('/address')">
          <div class="card-content">
            <i class="el-icon-location-outline icon"></i>
            <div class="text">
              <h3>地址管理</h3>
              <p>管理您的收货地址</p>
            </div>
            <i class="el-icon-arrow-right arrow"></i>
          </div>
        </el-card>

        <!-- 售后服务入口 -->
        <el-card class="box-card action-card" shadow="hover" @click.native="$router.push('/service')">
          <div class="card-content">
            <i class="el-icon-service icon"></i>
            <div class="text">
              <h3>售后与客服</h3>
              <p>退换货申请、在线客服</p>
            </div>
            <i class="el-icon-arrow-right arrow"></i>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 头像裁剪对话框 -->
    <el-dialog title="裁剪头像" :visible.sync="cropperVisible" width="600px" append-to-body>
      <div class="cropper-content">
        <div class="cropper" style="text-align:center">
          <vueCropper
            ref="cropper"
            :img="cropperImg"
            :outputSize="option.size"
            :outputType="option.outputType"
            :info="true"
            :full="option.full"
            :canMove="option.canMove"
            :canMoveBox="option.canMoveBox"
            :original="option.original"
            :autoCrop="option.autoCrop"
            :fixed="option.fixed"
            :fixedNumber="option.fixedNumber"
            :centerBox="option.centerBox"
            :infoTrue="option.infoTrue"
            :fixedBox="option.fixedBox"
            style="height: 400px;"
          ></vueCropper>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cropperVisible = false">取 消</el-button>
        <el-button type="primary" @click="finishCrop" :loading="uploading">确认并上传</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { userApi } from '../api'
import Avatar from '@/components/Avatar.vue'
import { VueCropper } from 'vue-cropper'
import axios from 'axios'

export default {
  name: 'UserProfile',
  components: {
    Avatar,
    VueCropper
  },
  data() {
    return {
      userForm: {
        id: null,
        username: '',
        nickname: '',
        email: '',
        phone: '',
        country: '',
        avatar: ''
      },
      // 裁剪相关配置
      cropperVisible: false,
      cropperImg: '',
      uploading: false,
      option: {
        img: '', // 裁剪图片的地址
        info: true, // 裁剪框的大小信息
        outputSize: 0.8, // 裁剪生成图片的质量
        outputType: 'jpeg', // 裁剪生成图片的格式
        canScale: true, // 图片是否允许滚轮缩放
        autoCrop: true, // 是否默认生成截图框
        autoCropWidth: 200, // 默认生成截图框宽度
        autoCropHeight: 200, // 默认生成截图框高度
        fixedBox: false, // 固定截图框大小 不允许改变
        fixed: true, // 是否开启截图框宽高固定比例
        fixedNumber: [1, 1], // 截图框的宽高比例
        full: true, // 是否输出原图比例的截图
        canMoveBox: true, // 截图框能否拖动
        original: false, // 上传图片按照原始比例渲染
        centerBox: true, // 截图框是否被限制在图片里面
        infoTrue: true // true 为展示真实输出图片宽高 false 展示看到的截图框宽高
      }
    }
  },
  computed: {
    ...mapState(['user']),
    uploadHeaders() {
      return {
        Authorization: 'Bearer ' + this.$store.state.token
      }
    },
    uploadAction() {
      // 我们的 UploadController 映射路径是 /upload
      // vue.config.js 配置了 /api 代理到 http://localhost:8080 并重写了路径（去掉了 /api）
      // 所以请求 /api/upload/avatar 会被转发到 http://localhost:8080/upload/avatar
      return '/api/upload/avatar'
    }
  },
  created() {
    this.initUserData()
  },
  methods: {
    initUserData() {
      if (this.user) {
        this.userForm = { ...this.user }
      }
    },
    // 选择文件后触发
    handleFileChange(file) {
      const isJPG = file.raw.type === 'image/jpeg' || file.raw.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG/PNG 格式!');
        return false;
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!');
        return false;
      }

      // 将文件转化为base64
      const reader = new FileReader();
      reader.onload = (e) => {
        this.cropperImg = e.target.result;
        this.cropperVisible = true;
      }
      reader.readAsDataURL(file.raw);
    },
    // 确认裁剪并上传
    finishCrop() {
      this.$refs.cropper.getCropBlob((data) => {
        this.uploading = true;
        const formData = new FormData();
        // 生成文件名
        const fileName = 'avatar_' + new Date().getTime() + '.jpg';
        formData.append('file', data, fileName);
        
        // 手动上传
        axios.post(this.uploadAction, formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
            ...this.uploadHeaders
          }
        }).then(res => {
          this.uploading = false;
          // axios 返回的数据在 res.data 中，而后端 Result 结构也在 data 中
          // 所以这里可能是 res.data.code 或者 res.data.data.code，取决于 axios 拦截器
          // 通常 axios 直接返回 response 对象，所以 res.data 是后端返回的 Result
          const result = res.data;
          
          if (result.code === 200) {
            this.cropperVisible = false;
            // 后端返回的 URL 是 /upload/avatar/xxx.jpg
            // 添加时间戳防止浏览器缓存
            this.userForm.avatar = result.data.url + '?t=' + new Date().getTime()
            this.$message.success('头像上传成功，请点击"保存修改"以生效')
          } else {
            this.$message.error(result.message || '头像上传失败')
          }
        }).catch(err => {
          this.uploading = false;
          console.error('上传失败:', err);
          let message = '头像上传失败，请稍后重试';
          if (err.response) {
            if (err.response.status === 401) {
              message = '上传失败：登录已过期，请重新登录';
            } else if (err.response.status === 403) {
              message = '上传失败：无权限操作';
            } else if (err.response.status === 413) {
              message = '上传失败：文件大小超过限制';
            }
          }
          this.$message.error(message);
        });
      })
    },
    async updateProfile() {
      try {
        // 移除头像 URL 中的时间戳，避免保存到数据库
        const formData = { ...this.userForm }
        if (formData.avatar && formData.avatar.includes('?')) {
          formData.avatar = formData.avatar.split('?')[0]
        }
        
        await userApi.updateUser(formData)
        this.$message.success('保存成功')
        
        // 更新 Vuex 中的用户信息
        // 关键：这里需要使用 formData (干净的 URL)，并手动加上时间戳给 Vuex
        // 这样当前会话的 Avatar 组件能感知到 src 变化并刷新
        const currentUserWithTimestamp = { ...formData }
        currentUserWithTimestamp.avatar = formData.avatar + '?t=' + new Date().getTime()
        
        this.$store.commit('setUser', currentUserWithTimestamp)
        
        // 更新本地存储 (使用干净的数据)
        localStorage.setItem('user', JSON.stringify(formData))
        
        // 刷新组件数据
        this.userForm = { ...currentUserWithTimestamp }
        
      } catch (error) {
        this.$message.error('保存失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
}

.profile-container {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.profile-card {
  flex: 2;
  min-width: 400px;
}

.right-section {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-info {
  display: flex;
  gap: 40px;
}

.avatar-section {
  text-align: center;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
}
.avatar {
  display: block;
}
.avatar-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.info-form {
  flex: 1;
  max-width: 500px;
}

.action-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.action-card:hover {
  transform: translateY(-5px);
}

.card-content {
  display: flex;
  align-items: center;
}

.icon {
  font-size: 32px;
  color: #409EFF;
  margin-right: 15px;
}

.text h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
}

.text p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.arrow {
  margin-left: auto;
  color: #C0C4CC;
}
</style>