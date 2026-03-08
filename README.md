# legacy
温商毕业设计

# 上传GitHub流程
git add .
git commit -m "v1.1 修改介绍"
git tag v1.1
git push
git push origin v1.1

# 启动项目流程
1. 启动后端服务
```bash
mvn spring-boot:run  (请在项目根目录 e:\Programs\legacy 下执行)
```
后端服务将在 `http://localhost:8080` 启动

2. 启动前端服务
```bash
npm run serve  (请在项目根目录 e:\Programs\legacy\frontend 下执行)
```
前端服务将在 `http://localhost:8081` 启动