# QLIT 自建后端

零依赖 Node.js 服务，同时承担三件事：

1. **应用更新**：`GET /update.json`（与现有 App 的 UpdateManager 契约完全一致，允许跨域读取，官网用它显示最新版本）、`GET /apk/<文件名>`（支持断点续传）、`GET /latest.apk`（固定地址，302 跳到当前线上安装包）
2. **匿名统计**：`GET /ping`（设备品牌/型号/系统/版本 + 同意结果，见 App 内 `UsageStatsReporter`）
3. **管理后台**：浏览器打开 `https://api.kevinxu.cn/` → 输入管理口令 → 网页上直接上传安装包、发布更新日志、查看统计（含同意/拒绝占比），全程不需要登录服务器面板

管理页模板在 `admin-page.js`，部署时需要和 `backend.js` 放在同一目录。

## 部署

```bash
# 服务器上（Node.js 16+）
cd server
ADMIN_TOKEN=换成你自己的口令 PORT=8080 node backend.js

# 建议用 pm2 常驻
pm2 start backend.js --name qlit-backend --env production
```

环境变量：

| 变量 | 默认 | 说明 |
|---|---|---|
| `PORT` | 8080 | 监听端口 |
| `ADMIN_TOKEN` | change-me | 管理口令，**务必修改** |
| `DATA_DIR` | ./data | 数据目录（pings.jsonl / state.json / apks/） |
| `PUBLIC_BASE` | https://api.kevinxu.cn | 对外基址，用于拼 apkUrl |

nginx 反代示例（443 → 8080）：

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr; # 覆盖式：防客户端伪造 XFF 绕过限速
    client_max_body_size 300m;   # APK 上传
}
```

## 端点一览

| 方法 | 路径 | 鉴权 | 用途 |
|---|---|---|---|
| GET | /ping | 无 | 匿名统计上报（App 调用） |
| GET | /update.json | 无 | 更新检查（App 调用；带 `Access-Control-Allow-Origin: *`，官网也读它） |
| GET | /apk/:file | 无 | 安装包下载（支持 Range） |
| GET | /latest.apk | 无 | 302 跳到当前线上安装包（官网下载按钮和二维码） |
| GET | / 、/admin | 无 | 管理网页（数据接口才需要口令） |
| GET | /admin/state | 口令 | 当前发布状态 + 已上传包列表 |
| PUT | /admin/apk?name= | 口令 | 流式上传 APK |
| POST | /admin/publish | 口令 | 发布版本（写 state.json） |
| GET | /stats.json | 口令 | 统计聚合 JSON |
| GET | /admin/pings.csv | 口令 | 原始 ping 导出 CSV |

## 数据口径

- **设备数**：按去重 uid 统计；同意/拒绝按每台设备最新一次表态计数；
- **品牌/系统/机型**：按设备去重后的最终值；**App 版本**按 ping 次数（近似日活构成）；
- 拒绝统计的用户只上报 uid + 版本 + 拒绝结果，不含任何设备信息。
