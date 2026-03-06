# 问题报告：个人中心头像上传失败及图片无法显示

## 1. 问题描述
**现象**：用户在个人中心尝试上传头像时，虽然提示上传成功，但头像区域显示为破损图标或不更新。刷新页面后，头像未持久化。后期还出现了上传接口 404 和构建失败的问题。

**期望结果**：
1. 用户选择图片并裁剪后，点击上传能成功保存。
2. 上传成功后，页面应立即显示新头像。
3. 刷新页面或重新登录后，头像依然保持为最新上传的图片。
4. 右上角导航栏头像应与个人中心同步更新。

**实际结果**：
1. 初期：点击上传无反应，或提示上传成功但图片显示为“裂开”的图标（404）。
2. 中期：控制台报错 `POST /upload/avatar 404`，因为前端代理路径与后端接口不匹配。
3. 后期：图片上传成功，但无法访问（`GET /upload/avatar/xxx.jpg 404`），因为 Windows 环境下静态资源映射配置失效。

## 2. 重现步骤
1. 启动后端服务 (`mvn spring-boot:run`) 和前端服务 (`npm run serve`)。
2. 登录系统，进入“个人中心”页面 (`/profile`)。
3. 点击头像区域或“点击上传头像”按钮。
4. 选择一张本地 JPG/PNG 图片。
5. （如果已修复裁剪功能）在裁剪弹窗中点击“确认并上传”。
6. 观察页面提示及头像显示情况。
7. 按 `F5` 刷新页面，观察头像是否回退。

## 3. 错误日志/截图
**Console 错误日志**：
* **上传接口 404**：
  ```
  POST http://localhost:8081/upload/avatar 404 (Not Found)
  ```
  或
  ```
  POST http://localhost:8081/api/upload/avatar 404 (Not Found)
  ```
* **图片加载 404**：
  ```
  GET http://localhost:8081/upload/avatar/20240227...jpg 404 (Not Found)
  Avatar load error: /upload/avatar/20240227...jpg
  ```
* **Maven 构建错误**（环境问题）：
  ```
  [ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin...
  MojoExecutionException
  ```

## 4. 建议或初步分析
1. **路径映射问题**：前端请求的 `/upload` 路径可能未正确通过 `vue.config.js` 代理到后端，或者后端 `Controller` 的 `@RequestMapping` 路径与前端不一致。
2. **静态资源映射失效**：Spring Boot 的 `WebMvcConfig` 中 `addResourceHandlers` 配置在 Windows 环境下对 `file:` 协议的路径解析可能存在兼容性问题，导致物理文件存在但无法通过 URL 访问。
3. **缓存问题**：浏览器对同名 URL（如用户头像 URL 不变）有强缓存策略，导致新图片上传后视图不更新。
4. **状态同步缺失**：前端 Vuex 和 `localStorage` 未在上传成功后同步更新头像 URL，导致刷新后数据丢失。

## 5. 解决办法
我们采取了“全栈路径统一 + 手动文件流”的稳健方案：

1. **后端改造 (`UploadController.java`)**：
   * **弃用静态映射**：不再依赖脆弱的 `WebMvcConfig` 静态资源映射。
   * **手动读取流**：新增 `GET /upload/avatar/{filename}` 接口，使用 `FileInputStream` 直接读取磁盘文件并写入 `HttpServletResponse` 输出流。
   * **硬编码路径**：将文件存储路径统一固定为 `D:/upload/overseas-purchase/`，避免环境差异导致的路径错误。
   * **路径规范**：上传接口和读取接口统一在 `/upload` 路径下，前端请求带上 `/api` 前缀（由后端 `context-path` 处理）。

2. **前端代理调整 (`vue.config.js`)**：
   * 简化代理配置，移除复杂的 `pathRewrite`，统一将 `/api` 开头的请求转发到后端 `8080` 端口。

3. **前端交互优化 (`UserProfile.vue`)**：
   * **集成 vue-cropper**：引入图片裁剪功能，提升用户体验。
   * **缓存穿透**：在上传成功后的图片 URL 后追加时间戳 `?t=...`，强制浏览器重新加载图片。
   * **状态同步**：上传成功后，手动更新 Vuex 和 `localStorage` 中的用户信息。

4. **环境修复**：
   * 针对 Maven 报错，确认是 Java 版本与 Maven 版本兼容性问题，通过调整环境或依赖解决。

## 6. 最终效果
1. **上传顺畅**：用户可以顺利选择图片、裁剪并上传，全程无报错。
2. **显示正常**：上传完成后，新头像立即显示，且右上角导航栏头像同步更新。
3. **持久化成功**：刷新页面或重新登录，头像依然是最新上传的图片。
4. **兼容性强**：手动流式读取方案彻底解决了 Windows 下静态资源路径映射不稳定的问题。
