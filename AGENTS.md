# AGENTS.md

## 构建
- 模块结构（KMP）：`composeApp` 是 KMP 库模块（`commonMain`/`androidMain`/`iosMain`），`androidApp` 是出 APK 的薄壳。共享代码放 `commonMain`，Android 专属（mockrun、NFC、Shizuku、widget、通知）放 `androidMain`。
- 编译：`./gradlew :androidApp:assembleDebug`；只编 Kotlin 用 `:composeApp:compileAndroidMain`
- 单测：`./gradlew :composeApp:testAndroidHostTest`（源集 `androidHostTest`；设备测试在 `androidDeviceTest`）
- iOS framework 验证：`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- 只打 `arm64-v8a` 包：`androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk`
- Windows：系统默认 Java 是 1.8，Gradle 需要 JDK 21：
  `JAVA_HOME="D:\Program Files\Android\Android Studio\jbr" GRADLE_USER_HOME="D:\AndroidSDK\gradle-home" ./gradlew :androidApp:assembleDebug`
- macOS（移动硬盘环境）：SDK 在 `/Volumes/Maxsun/Android/sdk`（`local.properties` 指定）；`~/.konan` 软链到 `/Volumes/Maxsun/konan-data`，各模块 `build/` 软链到 `/Volumes/Maxsun/qlit-build/`——内置盘空间不足，别让输出写回内置盘。
- Windows 本机跑着 Watt Toolkit（Steam++），它 MITM `github.com`（Gradle wrapper 的 zip 就发布在 GitHub Releases），
  其 CA `SteamTools Certificate` 在 Windows 信任库但不在 Java 的——Java 报 `PKIX path building failed` 时，
  从 `Cert:\LocalMachine\Root` 导出该 CA 导进 `jbr\lib\security\cacerts`（storepass `changeit`，已导入 alias `steamtools-mitm`）。

## 设备
- 真机装的是正式签名版，debug 包直接 install 会签名冲突，不要擅自卸载（会丢用户数据）。
- 模拟器：`D:/AndroidSDK/emulator/emulator -avd QlitWirelessUi37 -no-window -no-snapshot-save`（API 37，带 arm64 转译）。
  模拟器有 `wm size` 覆盖 840x1800，`adb shell input tap` 要用这个坐标系。
- Git Bash 下 adb 推文件要 `export MSYS_NO_PATHCONV=1`。

## UI 约定
- 只有一套界面（校徽红重设计），新代码用 `ui/design`（Q* 组件、`QIcons`）和 `ui/theme`（`Q.colors`、`QType`、`QlitHues`）。
- 红色（`Q.colors.seal`）只用于"此刻"和需要用户处理的事；控件主色是墨色。
- 进度与待办见 `UI_REFACTOR_TODO.md` 末尾"校徽红重设计"一节。
