# Miuix 参考速查

本项目的 Miuix（`top.yukonga.miuix.kmp`）实战备忘。来源：官方文档站 `docs/`、仓库源码目录、以及本项目的实际调用。后续写代码优先查这里，避免每次重新翻文档。

> 仓库：`compose-miuix-ui/miuix`（别名 `miuix-kotlin-multiplatform/miuix` 会重定向）
> 文档站为 VitePress，源在仓库 `docs/`，分中英（`docs/` 英文、`docs/zh_CN/` 中文）。
> 库自述："This library is experimental. APIs may change without notice." —— 升级小版本也要回归。

---

## 1. 版本与坐标

- 本项目当前：**`0.9.3`**（`gradle/libs.versions.toml` → `miuix = "0.9.3"`）。
- Maven Central 当前发布版：**`0.9.3`**。
- 依赖基线：Kotlin `2.4.0` / Compose Multiplatform `1.11.1`。本项目使用 Compose BOM `2026.05.01`，并显式对齐 Navigation3、NavigationEvent、Activity 与 Lifecycle。
- Group：`top.yukonga.miuix.kmp`。Android-only 项目用 `-android` 后缀的 artifact（本项目就是这么引的）：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-android:<version>")
implementation("top.yukonga.miuix.kmp:miuix-preference-android:<version>")
implementation("top.yukonga.miuix.kmp:miuix-icons-android:<version>")
implementation("top.yukonga.miuix.kmp:miuix-blur-android:<version>")
```

### 8 个模块（对应仓库 8 个子目录）

| artifact | 说明 | 本项目是否用 |
| --- | --- | --- |
| `miuix-ui` | 核心 UI 组件（基础控件 + theme + layout + overlay） | ✅ |
| `miuix-core` | UI 与形状等模块共享的底层 | 间接（`miuix-ui` 依赖） |
| `miuix-preference` | 设置页组件，依赖 `miuix-ui` | ✅ |
| `miuix-icons` | 扩展图标，可独立用 | ✅ |
| `miuix-blur` | 模糊/液态玻璃，可独立用 | ✅ |
| `miuix-squircle` | 连续圆角形状，可独立用（API 33+ shader，以下自动降级 `RoundedCornerShape`） | 间接 |
| `miuix-navigation3-ui` | 基于 AndroidX Navigation3 的 Miuix 页面转场与 UI 支持 | ✅（工具页路由） |
| `miuix-shader` | 底层 runtime shader / RenderEffect 抽象（`miuix-blur` 内部基于它） | 间接 |

---

## 2. 主题系统（`miuix-ui/.../theme`）

主题由 `MiuixTheme` + `ThemeController` 驱动，**和 Material3 的 `ColorScheme` 完全独立**。

### 入口三选一

```kotlin
// (a) 直接给 colors —— 最自由
MiuixTheme(colors = lightColorScheme()) { /* ... */ }

// (b) Controller 管模式（推荐）
val controller = remember { ThemeController(ColorSchemeMode.System) }
MiuixTheme(controller = controller) { /* ... */ }

// (c) Controller + 动态色 + 自定义种子
val controller = remember {
    ThemeController(
        ColorSchemeMode.MonetSystem,
        keyColor = Color(0xFF3482FF),            // null = 用系统壁纸色
        paletteStyle = ThemePaletteStyle.TonalSpot,
        colorSpec = ThemeColorSpec.Spec2021,
    )
}
```

### `ColorSchemeMode`（6 个）

| 值 | 含义 |
| --- | --- |
| `System` / `Light` / `Dark` | 跟随系统 / 强制浅 / 强制深 |
| `MonetSystem` / `MonetLight` / `MonetDark` | 动态色 + 对应明暗 |

### `ThemePaletteStyle`

`TonalSpot`（默认，Material You 平衡色调）/ `Neutral`（低彩）/ `Vibrant`（高彩）/ `Expressive`（大胆）这 4 个**支持 `Spec2025`**；`Rainbow`/`FruitSalad`/`Monochrome`/`Fidelity`/`Content` 只支持 `Spec2021`，传 `Spec2025` 会自动降级。

### `ThemeController` 属性

`colorSchemeMode` / `lightColors` / `darkColors` / `keyColor` / `colorSpec` / `paletteStyle` / `isDark`（`null` 跟随系统，非 null 强制覆盖）。

### `MiuixTheme` 读取当前主题

```kotlin
val colors = MiuixTheme.colorScheme        // Colors
val textStyles = MiuixTheme.textStyles     // TextStyles
val mode = MiuixTheme.colorSchemeMode      // ColorSchemeMode?
val isDynamic = MiuixTheme.isDynamicColor  // Boolean
```

### 颜色角色（关键差异）

Miuix `Colors` **比 Material3 多一组细分 disabled 角色**，桥接时要注意：

- `primary` / `onPrimary` / `primaryVariant` / `onPrimaryVariant`
- `error` / `onError` / `errorContainer` / `onErrorContainer`
- `disabledPrimary`（switch/btn/slider 通用禁用）
- **`disabledPrimaryButton`**（按钮专用）/ **`disabledPrimarySlider`**（滑块专用）—— M3 没有对应，需要自己派生
- `secondary` / `onSecondary` / `secondaryVariant` / `onSecondaryVariant` + 各自 `disabled*`
- `primaryContainer` / `onPrimaryContainer`
- `secondaryContainer` / `onSecondaryContainer` / `secondaryContainerVariant` / `onSecondaryContainerVariant`
- `tertiaryContainer` / `onTertiaryContainer` / `tertiaryContainerVariant`
- `background` / `onBackground` / `surface` / `onSurface` …

完整默认值见 `docs/guide/colors.md`（浅色/深色 ARGB 全表）。本项目把这些角色桥接到 M3 `ColorScheme` 的逻辑在 `ui/theme/QlitTheme.kt` 的 `MiuixQlitTheme`。

---

## 3. Scaffold 与弹窗体系 ⚠️ 重点

### 3.1 Scaffold

`top.yukonga.miuix.kmp.basic.Scaffold`。slot：`topBar` / `bottomBar` / `floatingActionButton`(+`floatingActionButtonPosition`) / `floatingToolbar`(+`floatingToolbarPosition`) / `snackbarHost` / `popupHost` / `content`。

- `popupHost` 默认 `{ MiuixPopupHost() }`。**每个 Scaffold 自带隔离的 popup 状态**，嵌套或并列 Scaffold 无需手动配，也不会互相打架。
- `contentWindowInsets` 默认 `systemBars.union(displayCutout)`。
- `FabPosition`：`Start` / `Center` / `End` / **`EndOverlay`**（末者覆盖在 bottomBar 上）。
- `snackbarHost` 文档原话"Miuix does not provide this component" —— slot 留给你塞 Material 的 `SnackbarHost`。

### 3.2 Overlay* vs Window* —— 两套对偶弹窗

**这是 Miuix 最容易误用的一层。** 命名规律极严格，选错就弹不出来。

| 类型 | Overlay* | Window* |
| --- | --- | --- |
| Dialog | `OverlayDialog` | `WindowDialog` |
| DropdownMenu | `OverlayDropdownMenu` | `WindowDropdownMenu` |
| DropdownPreference | `OverlayDropdownPreference` | `WindowDropdownPreference` |
| SpinnerPreference | `OverlaySpinnerPreference` | `WindowSpinnerPreference` |
| ListPopup | `OverlayListPopup` | `WindowListPopup` |
| BottomSheet | `OverlayBottomSheet` | `WindowBottomSheet` |
| CascadingListPopup | `OverlayCascadingListPopup` | `WindowCascadingListPopup` |

**唯一本质区别 —— 是否需要 Scaffold 提供 `MiuixPopupHost`：**

- **`Overlay*`** → `requiresScaffoldHost: true`。**必须**在 `Scaffold` 内使用，靠 `Scaffold.popupHost` 渲染。脱离 Scaffold → 弹窗内容不显示（不报错，只是空白）。
- **`Window*`** → `requiresScaffoldHost: false`。用平台原生 `Dialog` 在**窗口层**渲染，**任意 composable 作用域都能用**，自带大屏优化（宽屏居中缩放淡入 / 窄屏底部滑入）。

选型经验：
- 页面主体在 `Scaffold` 里 → 优先 `Overlay*`（和页面共用坐标系、过渡更连贯）。
- 顶层 Dialog / 不确定调用栈是否有 Scaffold / 全屏弹窗 → 用 `Window*`，零依赖。
- **本项目 `AdaptiveDialog` 目前统一走 `WindowDialog`**（见 `ui/components/AdaptiveControls.kt`）—— 这是稳妥选择，不依赖外层是否有 Scaffold。

### 3.3 统一关闭：`LocalDismissState`

所有 overlay 组件都向内容提供 `(() -> Unit)?`，内容里自己关弹窗：

```kotlin
import top.yukonga.miuix.kmp.theme.LocalDismissState

WindowDialog(show = visible, onDismissRequest = { visible = false }) {
    val dismiss = LocalDismissState.current
    TextButton(text = "Confirm", onClick = { dismiss?.invoke() })
}
```

比每个弹窗自己 `var show by remember` 更干净。

### 3.4 ✅ `LocalNavigationEventDispatcherOwner` workaround —— 已验证，必须保留

本项目 `MiuixQlitTheme`（`ui/theme/QlitTheme.kt`）里手动塞了：

```kotlin
val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(enabled = true, parent = null)
CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner) {
    MiuixTheme(controller = controller) { /* ... */ }
}
```

**已对照库源码（v0.9.2 + v0.9.3）验证：这段是正确的、且升级后仍必须保留。** 证据链：

1. 库 `miuix-ui/.../utils/MiuixPopupUtils.kt:529-536` 里，`PopupEntry` 用 `androidx.navigationevent` 的 `NavigationBackHandler` + `rememberNavigationEventState` 实现返回键关 popup（v0.9.2 和 v0.9.3 都有这段，行号一致）。
2. `NavigationBackHandler` 内部消费 `LocalNavigationEventDispatcherOwner`，需要有一个 root dispatcher。
3. 本项目的 Navigation3 host 只覆盖工具页路由；根级 popup 仍位于它之外，所以根主题继续提供 dispatcher owner。
4. 项目在 version catalog 中显式声明 `androidx.navigationevent:navigationevent-compose:1.1.2` 配合。

注释里写的 "Miuix popup host uses NavigationBackHandler" 一字不差就是事实。**不要删这段。**

---

## 4. 关键组件 API 速记

### NavigationBar / FloatingNavigationBar（`miuix-ui/.../basic`）

```kotlin
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
```

- `NavigationBar`：固定底栏。`color` 默认 `surface`、`showDivider` 默认 true、`mode`（`IconAndText` 默认 / `IconOnly` / `IconWithSelectedLabel`）。支持 2~5 项。
- `FloatingNavigationBar`：**库内置的悬浮胶囊底栏**（不是只能自绘）。`color` 默认 `surfaceContainer`、`cornerRadius`/`shadowElevation`/`horizontalAlignment`/`horizontalOutSidePadding` 可配、`showDivider` 默认 false。
  - → 本项目 TODO 写"默认用标准 Miuix 底栏，Liquid Glass 做可选实验样式"。标准 `FloatingNavigationBar` 已能满足大部分悬浮底栏需求，液态 Backdrop 是顶配。
- `NavigationBarItem`：`selected` / `onClick` / `icon: ImageVector` / `label: String` / `enabled` / `badge: (@Composable () -> Unit)?`。

### TextField（`miuix-ui/.../basic/TextField`）

两种重载：

1. **String/TextFieldValue 版**（本项目用这个）：`value` + `onValueChange`。
2. **state-based 版**（Compose 1.7+ 新 API）：`state: TextFieldState` + `inputTransformation`/`outputTransformation` + `lineLimits`。

共同参数坑点：
- `label: String` —— **必须是 String，没有 composable 重载**。本项目 P1-M7 已统一转 String。
- **`useLabelAsPlaceholder: Boolean`（默认 false）** —— 官方显式开关，控制 label 是否兼任 placeholder。对应 TODO 里"Miuix 用 label 兼任 placeholder"的 hack，其实有原生参数。
- **没有 `supportingText` slot**。本项目 `AdaptiveTextField` 把 supportingText 渲染到字段下方是正确做法。
- `leadingIcon` / `trailingIcon`：`@Composable (() -> Unit)?`。
- `readOnly` / `enabled` / `singleLine` / `maxLines` / `minLines` / `visualTransformation` / `keyboardOptions` / `keyboardActions` 都有。
- `TextFieldDefaults`：`CornerRadius` = 16.dp、`InsideMargin` = DpSize、`textFieldColors()`。

### WindowDialog 参数要点

`show: Boolean`（必填）/ `onDismissRequest: (() -> Unit)?` / `onDismissFinished`（隐藏动画结束才回调，中途取消不回调）/ `title: String?` / `summary: String?` / `backgroundColor` / `enableWindowDim`（默认 true）/ `outsideMargin` / `insideMargin` / `maxWidth` / `largeScreen: Boolean?`（null 自动按窗口宽度判断）/ `cornerRadius: Dp?` / `defaultWindowInsetsPadding`（默认 true，含 IME/nav/caption）/ `content`。

`DialogDefaults`：`MaxWidth` / `CornerRadius`（居中样式用）/ `outsideMargin` / `insideMargin` + `titleColor()` / `summaryColor()` / `backgroundColor()`。

### Dropdown 体系（`miuix-ui/.../basic` + `menu`）

```kotlin
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.WindowDropdownMenu     // 或 OverlayDropdownMenu
```

- `DropdownEntry(items: List<DropdownItem>)`，`DropdownItem(text, selected, onClick)`。
- `WindowDropdownMenu` / `OverlayDropdownMenu` 是 `BasicComponent` 的包装，点击弹出对应 popup。
- 多组：传 `List<DropdownEntry>`，组间自动加分隔线。`collapseOnSelection` 默认 `entries.size <= 1`（单组选中即关，多组保持打开）。
- 区别于 `*DropdownPreference`：`*Menu` 不持有单一选中下标，选中状态完全由每个 `DropdownItem.selected` 自己管 —— 适合动作菜单/多选。

### Surface / Text / Icon

别名导入，避免和 Material3 冲突：

```kotlin
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
```

本项目 `AdaptiveSurface` / `AdaptiveText` / `AdaptiveIcon`（`ui/components/AdaptiveControls.kt`）已在 Miuix 分支用这些别名。

### 完整 basic 组件清单（`miuix-ui/.../basic/*.kt`）

`Badge` / `BreadcrumbBar` / `Button` / `Card` / `Checkbox` / `ColorPalette` / `ColorPicker` / `Component`（即 `BasicComponent`，列表行基类）/ `Divider` / `Dropdown` / `FloatingActionButton` / **`FloatingToolbar`** / `Icon` / `IconButton` / `ListPopup` / `NavigationBar` / `NavigationRail` / **`NumberPicker`** / `ProgressIndicator` / **`PullToRefresh`** / `RadioButton` / `Scaffold` / **`ScrollBar`** / **`SearchBar`** / `Slider` / `SmallTitle` / `Snackbar` / `Surface` / `Switch` / `TabRow` / `Text` / `TextField` / **`Tooltip`** / `TopAppBar`。

（加粗的是容易被忽略、但 M3 没有直接对应的组件，需要时优先用原生版。）

---

## 5. miuix-blur / miuix-shader（液态玻璃）

本项目液态底栏（`ui/components/LiquidMiuixBottomBar` + `ui/components/liquid/*`）用到的：

```kotlin
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
```

- `isRenderEffectSupported`：运行时探测，不支持时必须降级为普通 Miuix 浮条。本项目 TODO 记录过 Android 16 上 Compose `GraphicsLayer` 触发栈溢出，`SafeInnerShadow` 等自定义图层要绕开递归。
- `LayerBackdrop` / `layerBackdrop` / `rememberLayerBackdrop`：分层采样源。
- `textureBlur` / `BlurColors` / `BlendColorEntry`：模糊 + 着色叠加。
- `miuix-shader` 是 `miuix-blur` 的底层依赖，一般不直接用。

---

## 6. 与本项目的对应关系（速查）

| 项目位置 | 用到的 Miuix 能力 | 备注 |
| --- | --- | --- |
| `ui/theme/QlitTheme.kt` | `MiuixTheme` + `ThemeController` + `ColorSchemeMode` + `ThemePaletteStyle` + `ThemeColorSpec` | Miuix 分支套 `MiuixTheme`，并把 36 个 M3 角色桥到 Miuix 色板 |
| `ui/theme/QlitTheme.kt` | `LocalNavigationEventDispatcherOwner` | 根级 popup 返回事件宿主，必须保留 |
| `ui/theme/QlitTypography.kt` | `MiuixTheme.textStyles` / `defaultTextStyles` | Token 清除 Material fontFamily |
| `MainActivity.kt` | `NavigationBar` / `NavigationRail` / `NavigationBarItem` + Miuix extended icons（`Recent`/`ListView`/`Settings`/`Info`） | 主导航，Miuix 分支 |
| `ui/components/LiquidMiuixBottomBar.kt` | `FloatingNavigationBar` 思路 + `miuix-blur` 全套 | 液态悬浮底栏（SukiSU 风格） |
| `ui/components/AdaptiveControls.kt` | `WindowDialog` / `MiuixButton` / `MiuixTabRow` / `MiuixSurface` / `MiuixText` / `MiuixIcon` | Adaptive 双主题分支收敛点 |
| `ui/components/AdaptiveTopAppBar.kt` | `TopAppBar` / `SmallTopAppBar` / `MiuixScrollBehavior` | |
| `ui/screens/AboutMiuixScreen.kt` 等 | `Scaffold` + `popupHost` 隔离 | |
| `ui/screens/ToolsMiuixScreen.kt` | `miuix-preference` 组件（`PreferenceGroup` 等） | Miuix 工具首页 |

---

## 7. 升级 / 维护注意

1. **Miuix 模块必须锁步升级**：`ui` / `icons` / `preference` / `blur` / `navigation3-ui` 共用同一 version catalog 版本。重点回归弹窗、模糊、主题切换与 Navigation3 返回行为。
2. ~~验证 `LocalNavigationEventDispatcherOwner` 是否还需要~~（已验证，**必须保留**，见 §3.4）。
3. **Kotlin / Compose MP 基线**：`0.9.3` 发布物使用 Kotlin `2.4.0` / Compose Multiplatform `1.11.1`；以 Maven 发布元数据为准，不要直接跟随 `main` 分支 badge。
4. **API 不稳定**：自述明说 "APIs may change without notice"。每次升级 diff 一下 `docs/components/` 里你用到的组件文档。
5. **`MaterialTheme.colorScheme` 残留**：颜色桥是过渡手段。长期目标是业务页直接用 `MiuixTheme.colorScheme`，逐步移除桥（见 `UI_REFACTOR_TODO.md` P0-M1）。
6. **参考实现**：官方 best-practices 点名 `InstallerX Revived`（switchable Material 3/Miuix）—— 和本项目双主题架构同款，值得对照。

---

## 参考链接

- 仓库：https://github.com/compose-miuix-ui/miuix
- Maven Central：https://central.sonatype.com/artifact/top.yukonga.miuix.kmp/miuix-ui
- 文档（英文）：仓库 `docs/guide/` + `docs/components/`
- 文档（中文）：仓库 `docs/zh_CN/`
- 在线 demo：`compose-miuix-ui.github.io/miuix-wasmJs/`
- 组件原始签名：IDE 里点进 `top.yukonga.miuix.kmp.*` 反编译，或仓库 `miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/`
