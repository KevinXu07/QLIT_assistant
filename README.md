# QLIT 小助手

**花有重开日，人无再少年。**
介于学校态度，本人将不再维护本项目，现在此开源全部代码，包括前后端以及app源码。

齐鲁理工学院校园助手——课表 + 校园工具，Kotlin Multiplatform / Compose Multiplatform，Android、iOS 共用一套 UI 与业务代码。

## 功能

- **课表**：周视图 / 今日视图，在线导入教务课表，课前提醒（桌面小组件 + 通知倒计时 + 小米灵动岛）
- **超星待办**：作业与考试列表
- **淘宝取件**：驿站取件码、身份码（WebView 登录态）
- **桌面小组件**：今日课程 + 取件/待办状态（Android Glance）
- **更新分发**：自建后端 APK 下发 + 匿名使用统计（可选关闭）

## 模块

```
composeApp/      KMP 库：commonMain（全部 UI + data 层）/ androidMain / iosMain
androidApp/      Android 壳模块，产出 APK
iosApp/          Xcode 工程 + QlitWidgets
server/          零依赖 Node.js 后端（api.kevinxu.cn：更新、统计、管理后台）
website/         官网静态页
promo/           宣传视频（Remotion）
prototype/       UI 原型
ui-audit/        真机界面审计截图
```

## 构建

```bash
# Android（需 JDK 21）
./gradlew :androidApp:assembleDebug

# 仅编 Kotlin
./gradlew :composeApp:compileAndroidMain

# 单测（host）
./gradlew :composeApp:testAndroidHostTest

# iOS framework（模拟器）
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

ABI 只出 `arm64-v8a`，minSdk 31 / targetSdk 37。


## 许可

[LICENSE](LICENSE)（GPL-3.0）
