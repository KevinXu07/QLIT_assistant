# 官网

纯静态页面，无框架、无外部依赖（字体、二维码库都在本地）。

```
index.html          首页（手机样机由 assets/js/site.js 用和 App 相同的数据渲染）
404.html            找不到页面时 EdgeOne 自动返回（资源用绝对路径）
edgeone.json        EdgeOne Pages 响应头：assets 强缓存一年，HTML 每次校验
assets/css/site.css
assets/js/site.js   主题、样机、时间轴、下载区（读 api.kevinxu.cn/update.json）
assets/js/qrcode.min.js   qrcode-generator 1.4.4
assets/fonts/       Barlow Semi Condensed 子集（仅拉丁字母、数字、标点），OFL 许可
assets/img/         图标各尺寸
```

## 本地预览

字体需要走 HTTP 才能加载，不要直接双击打开：

```
cd website
py -m http.server 8731
```

## 发布到 EdgeOne Pages

```
node website/build.mjs
```

生成 `build/qlit-website.zip`，在 EdgeOne Pages 控制台选「直接上传」上传这个 zip 即可。
打包时 assets 文件名会加上内容哈希，所以改完代码重新打包上传，用户不会读到旧缓存。
不要直接上传 `website/` 目录：那样文件名没有哈希，强缓存会让改动迟迟不生效。
