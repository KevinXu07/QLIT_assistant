# QLIT UI 整改待办

依据：`SukiSU-Ultra/miuix-ui-design-analysis.md`、SukiSU Ultra 当前 Miuix 实现，以及 QLIT 真机问题记录。

## 状态说明

- `[ ]` 未开始
- `[-]` 进行中
- `[x]` 已完成
- 每完成一项都需要：编译验证、真机验证、更新本文件。

## P0：稳定性和布局

- [x] **P0-1 底栏安全区和内容截断**
  - 将 Miuix 浮动底栏从 Scaffold 的占位式 `bottomBar` 中独立出来。
  - 页面内容显式预留浮条高度、间距和系统导航栏 Insets。
  - RMX5010 已复验主页、全部课程周视图、关于页；标准 Miuix 底栏也已增加独立安全区，末尾信息可完整滚动到。

- [x] **P0-2 真正的悬浮底栏 Host**
  - 使用全宽透明 Host + 居中浮条 + 明确层级。
  - 浮条不绘制页面背景，不裁剪 Pager 内容。
  - 保持 Material 底栏路径不变。

- [x] **P0-3 深色模式边缘高光**
  - 去除固定白色高光和固定黑色选中层。
  - 按浅色/深色主题使用不同的 Highlight、边框和指示器颜色。
  - 参照 SukiSU 的 `iosIndicatorSpecular` 和 `isInDarkTheme` 分支。

- [x] **P0-4 液态 Backdrop 链路**
  - 页面背景、底栏内容、选中指示器使用分层 Backdrop。
  - 保留 lens、vibrancy、折射、按压缩放和拖拽形变。
  - Runtime Shader 不支持时自然退化为普通 Miuix 浮条。

- [-] **P0-5 UI 模式切换稳定性**
  - RMX5010 真机已完成 Miuix → Material → Miuix 基础切换，进程和当前页面状态保持稳定；完整弹窗、Pager、进程重建回归仍待完成。
  - Material/Miuix 切换不闪退、不丢页、不改变业务保存时机。
  - 检查弹窗、Popup、Pager 和页面状态保存。

- [x] **P0-6 快速切换标签页状态一致性**
  - 底栏点击直接提交目标页，阻尼指示器只负责视觉跟随；避免快速点击时多个动画状态互相覆盖。
  - 分页动画期间忽略中间页的 settled 反馈，最后一次点击完成后强制落到准确页边界。
  - RMX5010 有线压力测试：8 轮、80ms 间隔，主页和全部课程作为最终目标均稳定落页，无应用崩溃。

## P0：Miuix 模式 Material 残留专项

本轮代码审计结论：当前是“外层 Miuix、业务内容混合 Material”。底栏、Miuix 关于页、Miuix 工具首页和大部分 Adaptive 控件已经切换成功，但课程页、工具子页、弹窗、图标和颜色桥仍有 Material 残留。

- [x] **P0-M1 Miuix 主题颜色桥完整化**
  - `QlitTheme.kt` 当前已在 Miuix 外层嵌套 `MaterialTheme`，且映射全部 36 个 M3 角色（含 `error`、`tertiary`、`surfaceBright`、`surfaceContainer*`、`outlineVariant`），Miuix 页面不会再混入默认紫色。
  - 颜色桥仍保留是因为部分业务页面内容仍用 `MaterialTheme.colorScheme`（经桥映射到 Miuix 色板）；待这些页面逐步改为 `MiuixTheme.colorScheme` 后可移除桥。
  - 完成后检查浅色、深色、Monet 和 AMOLED 对比度。

- [x] **P0-M2 全部课程页 Material 控件清零**
  - 已转换：`ExtendedFloatingActionButton`→`AdaptiveFloatingActionButton`、`FilterChip`/`AssistChip`→`AdaptiveFilterChip`、`OutlinedButton`→`AdaptiveButton`、`FilledTonalIconButton`→`AdaptiveIconButton`、5 组 `AlertDialog`→`AdaptiveDialog`、弹窗内 `OutlinedTextField`→`AdaptiveTextField`、`LinearProgressIndicator`→`AdaptiveLinearProgressIndicator`。
  - 周视图（`WeekGridToolbar`+`WeeklyScheduleGrid`）的 `Surface`→`GridSurface`（Miuix `MiuixSurface`）、`Text`→`GridText`（Miuix `MiuixText`），经 `GridSurface`/`GridText` helper 在 Miuix/Material 间分支。
  - 仍走颜色桥：空状态图标 `Surface`、`MaterialTheme.colorScheme` 派生色（toolbarBorder、课程色等）——这些经桥已映射到 Miuix 色板，视觉一致。

- [x] **P0-M3 浴室状态与余额流水 Miuix 化**
  - 浴室：`DisclaimerDialog` 和预约确认 `AlertDialog` 已改 `AdaptiveDialog`；`ShowerAreaCard` 占用 `LinearProgressIndicator` 已改 `AdaptiveLinearProgressIndicator`；`FilledTonalButton`/`Button`/`TextButton`（预约、查看预约、取消预约、开始服务、取消排队、密码显示）已改 Adaptive 版本。
  - 余额流水：`ScrollableTabRow`+`Tab`→`AdaptiveTabRow`；`CircularProgressIndicator`→`AdaptiveProgressIndicator`；`FilledTonalButton`→`AdaptiveButton`。
  - Miuix 模式下可见的业务 `Text`/`Surface`/`Icon` 已通过 Adaptive 分支收敛；`WalletBalanceAction` 已增加 Miuix 分支。剩余 Material 控件仅位于 Material-only 分支。

## P1：Miuix 业务页面残留控件

- [x] **P1-M4 主页弹窗和设置行**
  - 主页工具编辑和浴室选择 `AlertDialog` 已改 `AdaptiveDialog`（Miuix 走 `WindowDialog`）；弹窗内 `TextButton` 已改 `AdaptiveTextButton`。
  - `SegmentedDivider` 已增加 Miuix 分支；弹窗内标题、列表内容和浴室选择项已统一走 `AdaptiveText`。

- [x] **P1-M5 工具子页残留控件**
  - 超星：顶部退出改 `AdaptiveIconButton`、加载进度改 `AdaptiveLinearProgressIndicator`；登录页输入框 label 转 String。
  - 电费：特殊公寓字段、提醒阈值输入框 `OutlinedTextField`→`AdaptiveTextField`（支持 readOnly/supportingText）；低电量提醒卡片 `Surface` 仍走颜色桥。
  - 淘宝：身份码 `AlertDialog`→`AdaptiveDialog`（刷新按钮映射到 confirm）、身份码加载 `LinearProgressIndicator`→`AdaptiveLinearProgressIndicator`。
  - 三个子页的可见 `Text`/`Surface`/`Icon`、超星卡片和身份码卡片已通过 `AdaptiveText`/`AdaptiveSurface`/`AdaptiveIcon` 收敛；Material-only 分支保持原样。

- [-] **P1-M6 Material 图标和文字系统清理**
  - 主导航 `MainDestination.miuixIcon` 已从 Material Icons 改为 Miuix 扩展图标（`Recent`/`ListView`/`Settings`/`Info`），悬浮/标准/Rail 三种底栏均受益。
  - `AdaptiveRadioButton`、主页权限卡、浴室状态页等 Miuix 分支仍使用 Material vector icon；由于 Miuix 库没有完全对应的 `CheckCircle`/`Warning` 等图标，列为可选视觉统一项。
  - 7 个业务屏已完成 `MaterialTheme.typography`→`QlitTypography` 全量替换；Miuix 字体族由 Token 清除 Material fontFamily。

- [x] **P1-M7 Adaptive 控件参数完整性**
  - `AdaptiveTextField` 的 Miuix 分支已补齐 `readOnly`、`keyboardActions`、`trailingIcon`、`visualTransformation`；`supportingText` 在 Miuix 下渲染到字段下方（Miuix TextField 无该 slot）；`placeholder` 仍只在 Material 生效（Miuix 用 label 作 placeholder）。
  - 移除了固定文本“输入内容”的 composable label 重载，所有调用方已转 String label。

### 当前确认已经走 Miuix 的部分

- 液态悬浮底栏、标准 Miuix 底栏和 Miuix 导航栏（主导航 `miuixIcon` 已用 Miuix 扩展图标）。
- `ExpressiveScaffold` 的 Miuix 分支、Miuix 顶栏和关于页。
- Miuix 工具首页、`TonalCard`/课程卡片的 Miuix 分支。
- `AdaptiveTopAppBar`、`AdaptiveButton`、`AdaptiveSwitch`、`AdaptiveProgressIndicator`、`AdaptiveDropdownField` 和 `AdaptiveSnackbarHost` 的主要 Miuix 分支。
- `AdaptiveControls.kt`：`AdaptiveDialog`（`WindowDialog`）、`AdaptiveLinearProgressIndicator`、`AdaptiveTabRow`（`MiuixTabRow`）、`AdaptiveFilterChip`（`MiuixButton` 胶囊）。
- AllCourses 的 FAB、5 组弹窗、日期/周次/导入 Chip、弹窗内输入框、周视图翻页按钮、空状态导入按钮。
- Today 的自定义主页卡片弹窗和浴室选择弹窗。
- Hydream 的免责声明、预约确认弹窗、占用进度条、预约/查看/取消/开始服务/取消排队按钮、密码显示切换。
- Wallet 的月份 TabRow、加载/重试圆形进度、重试按钮。

## P1：Miuix 设计系统

- [x] 建立 spacing、radius、typography、surface 和语义色 Token。
- [-] 统一 `6/8/12/16dp` 间距节奏和卡片圆角（核心页面已接入，业务弹窗仍在收敛）。
- [-] 统一 Miuix Icons 与 Material Rounded Icons 的笔触和填充风格（主导航和核心控件已接入）。
- [ ] 统一顶栏、卡片、Preference 行、Popup 和空状态。
- [ ] 默认使用标准 Miuix 底栏，Liquid Glass 作为可选实验样式。
- [ ] 检查浅色、深色、Monet、AMOLED 下的文字和状态色对比度。
- [ ] 统一搜索覆盖层、菜单、返回键和页面过渡行为。

## P1：业务交互和错误反馈

- [ ] 高风险开关统一 `value + pending + success/error` 状态。
- [ ] native/root 操作失败时回滚 UI 状态并显示明确错误。
- [ ] 备份、恢复、批量删除增加互斥、进度和失败反馈。
- [ ] 破坏性操作补充清晰的确认正文和影响范围。
- [ ] 统一单选、多选、Switch、Dropdown 的控件语义。
- [ ] 技术表单增加输入类型、范围校验和防误操作。

## P2：验证和质量

- [-] 真机验证四个一级页面的底部滚动和底栏交互（主页、全部课程、关于页已通过，实用工具需再补一次）。快速切换标签页已完成 80ms 压力回归，拖拽与进程重建仍待补验。
- [ ] 验证窄屏、横屏、平板/宽屏 Navigation Rail 断点。
- [ ] 验证底栏点击、拖拽、快速切页和进程重建。
- [ ] 清理 Kotlin、Lint、资源和 Compose 警告。
- [ ] 增加 UI mode、Insets、危险操作回滚和表单校验测试。

## 修改记录

| 日期 | 项目 | 状态 |
| --- | --- | --- |
| 2026-07-25 | 复审收尾：完成 7 个业务屏 `QlitTypography` 全量接入；清理 AllCourses、Hydream、Today 弹窗和工具子页剩余的 Miuix 可见 Material 内容；确认 `AdaptiveIcon` 两种重载和 Material-only 分支边界 | `compileDebugKotlin` + `assembleDebug` 均 BUILD SUCCESSFUL；剩余仅可选 Material vector icon，以及真机深色对比度、间距圆角审计 |
| 2026-07-25 | P2-3 Token 全量接入 + 对话框残留收尾：QlitTypography 补 headlineSmall 角色；7 个业务屏(AllCourses/Today/Hydream/Wallet/Taobao/Chaoxing/Electricity)共 155 处 `MaterialTheme.typography`→`QlitTypography`(sed 全量替换 + 补 import)；清掉复审残留：AllCourses 弹窗星期/周次设置(用 QlitTypography.labelMedium)+ online/clear/delete 对话框内容 Text、Hydream 开始服务/到号后/取消排队按钮内容、Today 浴室选择器 8 个 Text + 快捷工具对话框 SegmentedListItem 内容 | 编译+assembleDebug 通过；7 屏 0 处 MaterialTheme.typography；剩余 Material Text 仅在 title slot 与 Material-only 分支(CourseCard/ViewModeSelector/ToolsListScreen/WalletBalanceAction else 等，只 Material 模式渲染)。AdaptiveRadioButton 的 CheckCircle/RadioButtonUnchecked 仍是 Material 向量(Miuix 无对应，可选) |
| 2026-07-25 | P2 视觉统一：新增 `AdaptiveText`/`AdaptiveSurface` 通用组件（AdaptiveControls.kt）；ChaoxingScreen、ElectricityScreen 全量 `Text`→`AdaptiveText`、`Surface`→`AdaptiveSurface`；TaobaoPickupScreen 身份码卡 7 个 Text + Surface；HydreamScreen 全部 Miuix 可见 Text/Surface 转 Adaptive（ShowerAreaCard/ShowerDeviceCard/状态加载错误空/ReservationDetail/Queueing/浴位示意图单元格/登录页/免责与预约弹窗）——共约 40 个 Text + 8 个 Surface | Debug Kotlin 编译、assembleDebug 通过；剩余 Material Text 仅在 ToolsListScreen 的 Material-only 分支（只 Material 模式渲染）。待办：AdaptiveIcon、完整 Miuix Typography Token、真机深色对比度与间距审计 |
| 2026-07-25 | 第二轮 Material 残留清理：AdaptiveTextField 补齐 readOnly/keyboardActions/trailingIcon/visualTransformation、Miuix 下 supportingText 渲染到字段下方、移除 composable label 重载；SegmentedDivider 加 Miuix 分支；AdaptiveFilterChip 改 `icon: ImageVector?` 不再依赖 Material Icon 组件；AllCourses 周视图(WeekGridToolbar+WeeklyScheduleGrid)Surface→GridSurface→MiuixSurface、Text→GridText→MiuixText；Hydream WalletBalanceAction 分支；WalletScreen 标题卡/余额列表/空/错/加载态全部分支 MiuixText/MiuixIcon/MiuixSurface；Chaoxing 退出按钮→AdaptiveIconButton、顶部进度→AdaptiveLinearProgressIndicator；Taobao 身份码弹窗→AdaptiveDialog、进度→AdaptiveLinearProgressIndicator；Electricity 特殊公寓/提醒阈值 OutlinedTextField→AdaptiveTextField | Debug Kotlin 编译、assembleDebug 均通过；工具子页内联 Text/Surface、超星 Surface 卡片、身份码卡片仍走颜色桥(P2) |
| 2026-07-24 | 新增 `AdaptiveControls.kt`（`AdaptiveDialog`/`AdaptiveLinearProgressIndicator`/`AdaptiveTabRow`/`AdaptiveFilterChip`）；AllCourses 的 FAB/5 弹窗/Chip/TextField/周视图按钮、Today 的 2 弹窗、Hydream 的免责/预约弹窗/进度条/按钮、Wallet 的 TabRow/进度/按钮、主导航 miuixIcon 全部改 Adaptive 或 Miuix 原生 | Debug Kotlin 编译、assembleDebug 均通过；业务页内联 Text/Surface/Icon 仍走颜色桥，待后续收尾 |
| 2026-07-24 | Miuix 模式 Material 残留审计：确认主题颜色桥、课程页、浴室/余额流水、工具子页、Material 图标、Material Typography 和 AdaptiveTextField 参数丢失问题 | 审计完成；已新增 P0-M1～P1-M7 专项，尚未改代码 |
| 2026-07-24 | Material 主题与页面恢复到 GitHub 旧版：旧版页面放入 `ui/legacy` 独立路径，Material 使用原始 `Scaffold`、`NavigationBar`、页面转场和默认 Shapes；Miuix 保持现有实现 | Debug 编译/安装通过；RMX5010 真机完成 Miuix → Material → Miuix 基础切换，进程稳定 |
| 2026-07-24 | RMX5010 有线回归：悬浮/标准底栏、液态模糊开关、Miuix→Material→Miuix、关于页滚动、课程周视图滑到底、深色高光 | 无应用崩溃；标准底栏遮挡已修复；当前设备已恢复浅色并关闭保持唤醒 |
| 2026-07-24 | 真机回归发现标准 Miuix 底栏会遮住关于页末尾信息；增加标准底栏高度与系统导航栏安全区，继续保留悬浮底栏独立安全区 | 已编译并真机复验 |
| 2026-07-24 | 补齐 Miuix 原生输入框、下拉选择、进度指示器、悬浮操作按钮和更新弹窗；修复通知权限与 API 36 Live Update 调用保护 | Debug/Release Kotlin 编译、Debug APK、Lint 均通过；待设备重新上线安装回归 |
| 2026-07-24 | Miuix 结构收敛：拆分标准/悬浮/液态底栏，补齐 Miuix Snackbar、子页面底部安全区、响应式断点、滚动触感和部分原生控件 | Debug 编译与 assemble 通过；待设备重新上线完成真机回归 |
| 2026-07-23 | 建立整改清单，开始处理 P0-1/P0-2 底栏布局 | 进行中 |
| 2026-07-23 | 完成悬浮 Host、安全区、主题边缘修正；发现 Android 16 RenderThread 递归崩溃并关闭 Backdrop 默认路径 | 待继续验收 |
| 2026-07-23 | 改为内容层 overlay，移除 Miuix 底部占位；安全模式增加半透明表面和软阴影 | 真机已启动，待继续交互验收 |
| 2026-07-23 | 按 SukiSU 层级重接 Pager Backdrop、tabsBackdrop 和 CombinedBackdrop；有线 RMX5010 真机验证折射效果与进程稳定 | 已完成，液态效果默认开启，可在 Miuix 设置关闭 |
| 2026-07-23 | 对齐 SukiSU 的重力高光、按压进度、动态 lens、交互径向高光和隐藏采样层着色 | Kotlin 编译通过，待设备恢复后验收拖拽动画 |
| 2026-07-23 | 有线 RMX5010 真机完成液态底栏截图和 About→Tools 拖拽切换验收，进程稳定 | 已完成 |
| 2026-07-23 | 按 SukiSU FloatingBottomBar 完整移植阻尼拖拽、dropShadow、InteractiveHighlight、指示器 innerShadow 和精确采样层布局；移除自绘白色边框 | Debug 编译通过，设备当前锁屏，待解锁后复验 |
| 2026-07-23 | 真机 logcat 确认新增 InnerShadow GraphicsLayer 触发 Compose StackOverflowError；移除该不兼容自定义图层，保留 Backdrop、dropShadow、lens、交互高光和阻尼拖拽 | 修复版编译安装，冷启动进程稳定，待解锁后复验 |
| 2026-07-23 | 安全加回 SukiSU 按压内阴影：保留 8.dp × pressProgress 与 0.15 × pressProgress 参数，按原算法使用 Canvas saveLayer、偏移 Clear 形状、模糊和裁剪，避开 Compose GraphicsLayer 递归 | Debug/Release Kotlin 编译、安装及有线拖拽验证通过，进程稳定 |
| 2026-07-23 | 为 Miuix 悬浮底栏增加统一的 LocalFloatingBottomBarExtraPadding，让一级页面和工具子页面的滚动内容为悬浮层预留安全区，Material 路径不受影响 | Debug 编译、安装通过；About/AllCourses 真机滚动验证通过 |
| 2026-07-23 | 修正 AllCourses 的 ExtendedFloatingActionButton 与悬浮底栏重叠问题，并完成 Miuix → Material → Miuix 真机切换回归 | Debug assemble、安装通过；PID 20545 稳定，无 crash buffer 异常 |
| 2026-07-24 | 修正课程周视图与悬浮底栏的层级：内容铺到栏下方，滚动尾部单独预留安全区 | Debug 编译安装通过；周视图顶部覆盖、滑到底最后一行均已真机验证 |
| 2026-07-24 | 修正关于页应用图标与开发者信息混用：顶部使用系统应用图标，新增 Kevin 开发者信息卡片，并修复自适应图标加载崩溃 | Debug 编译安装通过；Miuix/Material 关于页真机验证通过，无新增应用崩溃 |
| 2026-07-24 | 修复快速切换标签页偶发停在两页之间：点击直接提交、Pager 程序切页目标锁定，并按 SukiSU 条件收紧液态选择器高光触发 | Debug 编译安装通过；RMX5010 8 轮 80ms 快速切换回归通过 |
| 2026-07-24 | 对比 SukiSU 与 RMX5010 截图，移除 Android 16 上会生成顶部灰带的安全 Canvas 内阴影蒙版；保留 Backdrop、lens、折射和动态高光 | A/B 真机验证：灰带消失，液态主体与交互保持稳定 |

## 当前阶段状态（2026-07-25）

底栏核心已经基本完成 SukiSU 风格移植，目前进入真机回归、内容安全区修正和视觉精修阶段。

### 已完成

- 真正悬浮在内容上方的底栏 Host。
- SukiSU 胶囊形布局、分层 Backdrop、vibrancy、lens 和折射效果。
- 阻尼拖拽、快速切页、按压缩放、交互高光、重力高光和 dropShadow。
- 按压内阴影的安全 Canvas 实现，避免 Android 16 上 Compose GraphicsLayer 栈溢出。
- Material 样式保留，Miuix/Liquid 样式可切换。
- Material 路径已切回 GitHub 旧版页面与主题；旧版源码与当前 Miuix 页面隔离，便于继续并行维护。
- 有线 RMX5010 真机完成 About → Tools 拖拽验证，Debug/Release Kotlin 编译通过。
- Miuix 业务页 Material 残留的可见控件已完成 Adaptive 收敛；`AdaptiveIcon`、`QlitTypography` 和 7 个业务屏 Typography 全量接入已完成。

### 待收尾

- 完成主页、工具页及工具子页面的底部内容安全区验证；About/AllCourses 已通过。
- 补完 Material/Miuix 快速切换、快速拖拽、进程重建等完整稳定性回归；标签页快速点击本轮已通过。
- 继续对齐 SukiSU 的模糊强度、边缘高光、动画曲线和不同深色背景表现。
- 完成横屏、窄屏和不同系统导航模式下的底栏验证。
- 可选：统一 Material vector icon 的资源来源；真机完成深色/AMOLED 对比度、间距和圆角审计。

## 合并前审查记录（2026-08-27）

审查范围：当前 `agent/miuix-material-closure` 分支，HEAD `11f5b6202ec1ac549e3bf2921063eacdb69cd1a4`；未使用历史较旧的 `main` 作为判断基线。结论：以下问题处理或确认前，不建议直接合并/发布。

- [ ] **P0 构建环境一致性**：`app/build.gradle.kts` 使用 `compileSdk = 37`，但当前 `D:\AndroidSDK` 仅安装到 `android-36.1`，需要安装 API 37 或调整编译 SDK。
- [ ] **P1 备份中的 WebView 登录态**：Manifest 开启 `allowBackup`，备份规则只排除了凭据 SharedPreferences；淘宝 WebView 的 `app_webview` Cookie 数据尚未明确排除，需要补充排除规则或关闭备份，并用实际备份/迁移流程确认。
- [ ] **P1 CDP 多页面选择**：`CdpClient` 的注释称会逐个尝试候选页面，但当前实现只连接一个 target；微信同时存在多个 WebView 时可能选中空白/无关页面，导致登录态抓取失败。
- [ ] **P2 Kotlin 诊断确认**：已有 `build-verify` Problems Report 报告 `SegmentedList.kt` 的 nullable `onClick` 类型诊断；虽然现有编译产物存在，但仍应以干净 CI 构建确认并清理该诊断。
- [ ] **P2 页面离开时的请求生命周期**：`TravelViewModel` 离开页面只取消 CDP 抓取，认证/提交请求仍可能在后台完成；需要明确保留后台提交的策略，避免重新进入时状态陈旧或重复提交。

验证记录：现有历史 `testDebugUnitTest` 产物共 65 个测试、0 失败；本轮未能完成全新构建，原因是 Gradle 需要对 SDK 盘缓存创建锁文件且当前环境拒绝该写入。工作树中的 AGP 临时改动、子模块状态和构建/截图产物不计入上述分支审查。

## 主题升级引导（2026-08-27）

新增 Miuix 主题升级弹窗：当当前版本高于 `1.0.5`，且检测到用户来自 `1.0.5` 及以下版本时显示一次；用户可立即切换到 Miuix，或选择暂不切换。旧版本没有保存版本号时，使用已有 UI 偏好作为兼容识别依据；已明确选择 Miuix 或已处理本次引导的用户不会重复提示。

## 隐藏工具签发解锁（2026-10-22，替代 2026-08-27 的深链方案）

「浴室预约 / 模拟定位 / NFC 打卡 / 出行登记」四个工具默认隐藏，由服务端为设备签发的校验码解锁（原 `qlit://travel-unlock` 深链方案已整体移除，`TravelEntryActivity`/`TravelEntryGate` 删除）：

- 设备码：App 首次启动生成 8 位随机码（去 0/O/1/I/L），存 `ui_preferences.device_unique_code`，在「设置 → 设备唯一码」里展示并可复制。
- 签发：发布中心「设备签发」卡片输入设备码，服务端用 ECDSA P-256 私钥（`DATA_DIR/device-license-key.pem`，首启自动生成）签出 `QLIT1.<设备码>.<时间戳b36>.<base64url签名>`，签名内容为 `qlit-device-v1|<设备码>|<unix秒>`。
- 验证：App 内嵌同密钥公钥（`DeviceLicense.PUBLIC_KEY_BASE64`，从管理页「App 公钥」复制），`DeviceLicense.activate` 验签 + 比对设备码后把签发码本体存入偏好；`isUnlocked` 每次重新验签，不落布尔标记，改偏好文件无法伪造。
- 隐藏面：工具列表四项、今天页快捷工具卡片与其自定义对话框、订阅浴室区块、`tool_destination` 深链直达，均由同一闸门控制。
- 撤销：「设备唯一码」对话框里「撤销签发」即重新隐藏。

## 校徽红重设计（2026-10-19，分支 `redesign/seal-ui`，未提交）

依据：`prototype/redesign.html`（三屏原型，用户已确认方向）。决策：Material 模式整体删除；Monet 动态取色删除；保留 SukiSU 液态玻璃底栏（选中色改校徽红）；课表页只保留周视图；工具子页全部按原型重做。

### 已完成

- [x] **删除 Material 模式**：`ui/legacy/*`、`Theme.kt`/`Color.kt`（Material 主题）、`UiMode`、`MiuixUpgradePrompt`/`MiuixThemeUpgradeDialog`、`AboutMiuixScreen.kt`、标准底栏与 NavigationRail 分支。各屏 `LocalUiMode` 判断已替换为常量，剩余死分支随子页重做清理。
- [x] **设计系统**
  - `ui/theme/QlitPalette.kt`：浅/深两套色板（暖白 bg、墨色 ink/ink2/ink3、校徽红 seal 只给"此刻"）；`Q.colors` 访问；`QlitHues` 工具/设置固定色相；`CoursePalette` 按黄金角分配课程色相，跳过 0°–50° 红橙区间。
  - `ui/theme/Oklch.kt`：OKLCH→sRGB；同一色相生成 container/accent/content/iconContainer/icon 五种角色，深色自动压暗。
  - `ui/theme/QlitFonts.kt`：数字字体 Barlow Semi Condensed（`res/font/`，许可证 `assets/licenses/BarlowSemiCondensed-OFL.txt`）+ `QType` 字号体系。
  - `ui/theme/QlitTheme.kt`：`ThemeMode`（跟随系统/浅色/深色，存 `ui_preferences.theme_mode`）；色板映射到 Miuix `Colors` 与 Material `ColorScheme`（控件主色为墨色，error 为校徽红）。
  - `ui/design/QIcons.kt`：原型同款线条图标（24 栅格、1.8 描边）。
  - `ui/design/QComponents.kt`、`QChrome.kt`：`QText`、`QCard`、`QGroup`/`QList`/`QRow`/`QSwitchRow`/`QRadioRow`、`QTintIcon`、`QBadge`、`QButton`、`QChip`、`QSegmented`、`QEmpty`、`QNotice`、`QStat`、`QProgressBar`、`QTopBar`（大标题随滚动折叠）、`QPage`、`QSheet`（窗口级底部弹窗）、`QDialog`、`QStatusBarScrim`。
  - 旧的 `Adaptive*`、`Segmented*`、`TonalCard`、`ExpressiveScaffold`、`MiuixPicker`、`UpdateDialog`、`FeedbackDialog`、`AppStartupDialogs` 已改为只走新设计，签名基本不变，子页可直接继承新样式。
- [x] **主导航**：今天 / 课表 / 工具 三个标签；"关于"收进头像个人页。设置页、关于页作为全屏覆盖页（`MainActivity` 的 `AppOverlay`，返回走 NavigationBackHandler）。
- [x] **今天页**（`TodayScreen.kt`）：日期+周次顶栏、头像；主角卡（下一节/正在上课/今天上完→下一天第一节/两周无课/未导入），校徽水印，倒计时，当天轨道；周条；时间轴（已上完变淡、正在上课红点脉动、空档提示）；快捷工具（状态+红色角标，长按自定义）；权限提示改为墨色低调样式。保留下拉刷新、实时通知/超级岛、主页卡片自定义、淘宝预热、电费/浴室刷新逻辑。
- [x] **课表页**（`AllCoursesScreen.kt`）：周网格、左右滑/箭头换周、回到本周、虚线"本周不上"、红色当前时间线、周末无课自动收起；"更多"弹窗（在线读取/链接/文件导入、添加课程、清除课表）；课程详情与编辑弹窗共用 `ui/schedule/CourseSheets.kt`。
- [x] **纯逻辑**：`ui/schedule/ScheduleLogic.kt`（主角卡状态、周日期、网格行与跨行、可见星期、周次描述），单测 `app/src/test/.../ui/schedule/ScheduleLogicTest.kt` 12 项通过。
- [x] **工具页**（`ToolsMiuixScreen.kt`）：校园生活/学习/实验功能分组，行尾状态读 `QlitWidgetCache`，待处理项红色。
- [x] **个人页/设置/关于**：`ProfileSheet.kt`、`SettingsScreen.kt`（深浅色、底栏液态玻璃、主页快捷工具、通知、匿名统计）、`AboutScreen.kt`（含字体许可证）。
- [x] **子页已按原型重做**：电费查询、超星待办、浴室余额流水（`WalletScreen.kt`）、NFC 打卡。
- [x] **剩余工具子页按 `prototype/tools.html` 重做**（用户已确认原型）：
  - 淘宝取件（`TaobaoPickupScreen.kt`）：摘要卡红色待取数+「出示身份码」主按钮（替代 FAB）；包裹卡大号取件码点卡复制；骨架/空状态/错误重试；登录与人机校验改为底部弹窗内嵌 WebView，成功自动收起；身份码底部弹窗（条形码白底卡、亮度开关挪入）。
  - 浴室预约（`HydreamScreen.kt`，死分支已清）：登录密码/验证码分段切换+60s 倒计时；余额卡（低于 ¥10 变红）；浴室行右侧大号空位数；浴位示意图按状态上色、点空格直接预约（9号楼1层男浴用内置布局，其余按编号网格）；红色「票」卡（喷头号+倒计时+进度条）；预约后可返回列表，顶部常驻红色横幅点回票，取消先确认；退出登录收进「更多」弹窗；满员底部「去排队」按钮。
  - 模拟定位（`CampusRunScreen.kt`）：地图全屏+玻璃圆钮；底部面板两档（收起=路线摘要+开始钮，展开=参数）；道次/我的路线/「自己画」一排胶囊；圈数步进器、速度旁写配速、漂移为 0 显示「关闭」；路线色跟底图（卫星白线/矢量墨线）；运行中显示累计距离、已用时、配速、剩余，红色方向箭头；未设模拟位置应用时提示卡置顶并禁用开始；路线缩放适配面板高度。
  - 出行登记（`QlitCaptureScreen.kt`）：竖向三步卡（完成折叠/当前展开/未开始置灰）；验证中页面日志默认收起；表单按时间/去向/联系分组，选择器全走底部弹窗（日历、时间滚轮、单选列表）；重复登记/超出推荐时间红色提示卡；确认弹窗复述警告；结果页单结论+两按钮；`TravelViewModel` 重验后保留已填内容；`WirelessAdbSetupStage.kt` 死代码删除，仅留 `WirelessDebuggingSettings.kt`。
- [x] `ScheduleViewModel.clock`：对齐整分钟的时钟，驱动倒计时和时间线。

### 待办

- [ ] 深色模式逐页截图检查（工具子页已查，其余页面待查）。
- [ ] 真机验证：手机上是正式签名版本，debug 包 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`；需决定卸载重装（会丢本地课表/登录数据）或用正式签名打 release 包。
- [ ] 提交并更新 `README`/截图。

### 验证记录

| 日期 | 内容 | 结果 |
| --- | --- | --- |
| 2026-10-19 | `compileDebugKotlin`、`assembleDebug`、`ScheduleLogicTest` | 全部通过 |
| 2026-10-19 | 模拟器 `QlitWirelessUi37`（API 37，`-no-snapshot-save`）装 debug 包，推入测试课表，截图检查今天页、课表页 | 与原型基本一致；修正了主角卡水印撑高、状态栏遮挡、课程色太接近红色 |
| 2026-10-19 | `compileDebugKotlin`、`testDebugUnitTest`、`assembleDebug`；模拟器截图检查淘宝取件（登录卡+网页弹窗）、浴室预约（登录两 tab）、模拟定位（提示卡/路线/运行统计/深浅色/图层切换）、出行登记（三步卡，深链解锁后） | 全部通过；修正了路线被底部面板盖住（缩放时扣除面板高度） |
