# ChatGPT Android APK 逆向分析

对官方 ChatGPT Android 客户端（`com.openai.chatgpt`）UI 实现的分析，重点是两处：

1. **设置页的「外观 / 主题切换」** 是如何实现的；
2. **聊天回复过程中出现的「智能界面」（Intelligent UI）** 是什么机制。

- 分析日期：2026-10-12
- 目标文件：`com.openai.chatgpt_1791789377928.apk`
- 目标版本：`versionName 1.2026.279` / `versionCode 2627909`
- 体积：82.95 MB ｜ dex：8 个（约 67 MB）｜ minSdk 29 / targetSdk 37
- 分析方法：静态分析（**未运行**该 APK，未连接其服务）

## 目录

- [一、核心结论](#一核心结论)
- [二、分析方法与证据等级](#二分析方法与证据等级)
- [三、APK 结构与 UI 技术栈](#三apk-结构与-ui-技术栈)
- [四、外观与主题系统](#四外观与主题系统)
- [五、DIL：智能界面引擎](#五dil智能界面引擎)
- [六、可借鉴的设计点](#六可借鉴的设计点)
- [七、复现步骤与工具链](#七复现步骤与工具链)
- [八、本次分析的边界](#八本次分析的边界)
- [九、图标体系](#九图标体系自研为主)
- [十、DSL 引擎的来源](#十dsl-引擎的来源自研构建在开源-valdi-之上)
- [十一、两处具体图标的追查](#十一两处具体图标的追查)
- [十二、DIL 引擎的完整架构](#十二dil-引擎的完整架构深挖)
- [十三、智能界面的运行时协议](#十三智能界面的运行时协议深挖)

---

## 一、核心结论

**ChatGPT 的 UI 既不是传统 Android View，也不是 Jetpack Compose，而是 Valdi** —— Snap 开源的
TypeScript → 原生 UI 框架（基座为 `com.snap.valdi.*`，OpenAI 在其上构建了 `com.openai.valdi.*`）。
界面逻辑以 TypeScript 编写、编译为 `.valdimodule` 打包进 APK，运行时由原生侧渲染。

两个问题的答案：

| 问题 | 答案 |
|---|---|
| 外观 / 主题切换怎么实现 | 控件是 `SettingsMenuRow`，点击时打开**系统原生菜单**（`NativeMenu`），菜单项选中后写回主题状态；主题值由三层体系 `OAITokens → OAIUITheme → ValdiUITheme` 解析注入 |
| 「智能界面」是什么 | 一套名为 **DIL** 的 DSL 引擎（`dil_core` / `dil_compiler` / `dil_renderer` / `dil_sandbox` / `dil_cel`）。服务端下发界面描述，客户端**沙箱求值 → 编译成渲染树 → 渲染为原生 UI**，并自带自动间距与容错 |

---

## 二、分析方法与证据等级

本次分析的每一项结论都标注了来源。等级含义：

| 等级 | 含义 |
|---|---|
| 🔒 **确定** | 直接读到了源码符号或文件字节，可复现 |
| 🔍 **确证** | 多个独立证据相互印证 |
| 💡 **推断** | 由符号命名与结构合理推测，**未读取实现体** |

关键突破点：**`.valdimodule` 的 magic 为 `28 B5 2F FD`，即 Zstandard 帧**。解压后得到的是
**未混淆的 JavaScript/TypeScript 源码**（类名、方法名、字符串常量完整保留），因此本报告大部分
结论属于 🔒 确定等级。

---

## 三、APK 结构与 UI 技术栈

### 3.1 顶层结构（🔒 确定）

| 路径 | 数量 | 说明 |
|---|---|---|
| `res/` | 2242 | Android 资源（仍存在，但 UI 主战场不在这里） |
| `assets/` | 2039 | **UI 与资源主战场** |
| `com/` | 542 | 顶层包目录 |
| `META-INF/` | 324 | 签名与依赖元数据 |
| `classes*.dex` | 8 | 约 67 MB |
| `lib/` | **不存在** | 没有传统 `.so` |

### 3.2 Valdi 原生模块（🔒 确定）

`assets/valdi_native_modules/`（13 个）—— 桥接到平台能力的原生模块：

```
com.openai.valdi.chatgpt.application.ChatGPTApplicationNativeModuleFactoryImpl
com.openai.valdi.audio.AudioPlayerNativeModuleFactoryImpl
com.openai.valdi.voice.VoiceServiceNativeModuleFactoryImpl
com.openai.valdi.lottie.LottieNativeModuleFactoryImpl
com.openai.valdi.filepicker.FilePickerNativeModuleFactoryImpl
com.openai.valdi.filesaver.MobileFileSaverNativeModuleFactoryImpl
com.openai.valdi.location.LocationNativeModuleFactoryImpl
com.openai.valdi.integrity.DeviceIntegrityNativeModuleFactoryImpl
com.openai.valdi.chatgpt.multiplayer.MultiplayerConnectionServiceNativeModuleFactoryImpl
com.snap.valdi.modules.http.StreamingHTTPClientNativeModuleFactoryImpl
com.snap.valdi.modules.webview.WebViewNativeModuleFactoryImpl
com.snap.valdi.modules.platformalert.PlatformAlertNativeModuleFactoryImpl
com.snap.valdi.modules.platformbrowser.PlatformBrowserNativeModuleFactoryImpl
```

### 3.3 属性绑定器（🔒 确定）

`assets/valdi_attributes_binders/`（7 个）—— 把 Valdi 的属性映射到原生控件：

```
com.openai.valdi.ui.ValdiRefreshControlAttributesBinder
com.openai.valdi.ui.ValdiSwitchAccessoryAttributesBinder
com.openai.valdi.ui.ValdiNativeIconButtonAttributesBinder
com.openai.valdi.dil.widgets.DILPopoverSheetViewAttributesBinder
com.openai.valdi.dil.widgets.DILSelectViewAttributesBinder
com.openai.valdi.lottie.LottieAnimatedImageViewAttributesBinder
com.snap.valdi.modules.webview.ValdiWebViewAttributesBinder
```

> 注意 `com.openai.valdi.dil.widgets.*` —— **DIL 组件有专属的原生绑定器**，即 DIL 不只是
> TS 层实现，还下沉到了原生控件（🔒 确定）。

### 3.4 三套并存的 UI 资源（🔒 确定）

```
assets/composeResources/   1087 项   ← Jetpack Compose Multiplatform 资源（仍在用）
assets/*.valdimodule/       84 个    ← Valdi 模块（当前 UI 主体）
assets/orbit-*             ~760 项   ← 3D 资产（meshes 56 / preset-fits 108 / thumbnails 533）
assets/valdi_api_version / valdi_core.valdimodule …
```

> 💡 **推断**：项目处于「Compose → Valdi」迁移的中间态，或两者按功能分工。

---

## 四、外观与主题系统

### 4.1 设置页的组件体系（🔒 确定）

全部位于 `oai_ui/src/components/settings/`：

| 组件 | 职责 |
|---|---|
| `SettingsRow` | 基础设置行（图标 + 标签 + 尾部内容） |
| `SettingsActionRow` | 可点击行 |
| **`SettingsMenuRow`** | **点击弹出菜单的行**（主题、强调色用它） |
| `SettingsToggleRow` | 开关行 |
| `SettingsSection` | 分组容器 |
| `SettingsList` | 列表 |
| `SettingsTextEntrySection` | 文本输入区 |
| `SettingsSearchHighlight` | 搜索高亮 |
| **`SettingsTheme`** | **设置页外观主题解析** |

设置页本身：`personalization_settings_ui/src/PersonalizationSettingsScreen`
（关联 `PersonalizationSettingsModels`、`PersonalizationSettingsRepository`、
`resolveSettingsTheme`、`renderAppearance`）。

### 4.2 主题切换控件的实现（🔒 确定）

`oai_ui/src/components/settings/SettingsMenuRow.js` 反解出的符号序列：

```
SettingsMenuRow
  ← native_menu/src/NativeMenu                     ★ 使用系统原生菜单
  ← ./SettingsActionRow
  Component.onRender:
    supportsNativeMenu / context                   ★ 先探测平台是否支持原生菜单
    beginComponent "SettingsActionRow"
      @SettingsAppearance                          ★ 注解声明外观
      Selection / accessibilityId / accessibilityValue
      disabled / label / onPress / onFallbackPress  ★ 有回退路径
      position / valueColor                        ← 尾部显示当前值
    endComponent
    NativeMenu.hasContext / setContext
      accessibilityLabel / actions / onSelect / Menu
```

**要点**

1. 该行**只负责显示标签与当前值**（`valueColor`），**不自己画菜单**；
2. 点击后交给 `NativeMenu`（原生菜单）弹出选项，选中后经 `onSelect` 回调；
3. 若平台不支持原生菜单，走 `onFallbackPress`（跨平台回退）；
4. 无障碍信息完整（`accessibilityLabel` / `accessibilityValue` / `accessibilityId`）。

> 这解释了截图中「强调色 🔵蓝色 ⌄」的形态：**值显示在行尾，菜单由原生层弹出**，
> 而不是在页面里塞一个下拉容器。

### 4.3 主题的三层体系（🔒 确定）

```
① 令牌层   oai_core/src/OAITokens        原子设计值
② 主题层   oai_core/src/OAIUITheme       语义组合
           oai_core/src/OAIAttributes    属性系统
           oai_core/src/OAITypography    排版
③ 平台层   valdi_ui_theme/src/ValdiUITheme   平台默认值（可被覆盖）
                    ↓
           oai_ui/src/components/settings/SettingsTheme
             resolveSettingsTheme / resolveSettingsColors
             resolveSettingsEditorAppearance / SettingsEditorPresentation
             resolveSettingsHorizontalInset / wideListHorizontalInset / wideListMinimumWidth
             valdi_ui/src/ControlMaterial       控件材质
```

### 4.4 默认主题的精确取值（🔒 确定）

从 `valdi_ui_theme` 模块（`DEFAULT_VALDI_UI_THEME` / `ValdiUIThemeProvider` /
`resolveValdiUITheme`）解出的原文常量：

| 角色 | 值 | 界面含义 |
|---|---|---|
| `canvas` | `#FFFFFF` | 页面画布 |
| `groupedSurface` | `#F3F3F3` | **分组卡片底色**（设置页的灰卡） |
| `border` | `#DDDDDD` | 分隔线 / 结构边框 |
| `accent` | `#111111` | 强调色（默认近黑，可切换） |
| `accentPressed` | `#333333` | 强调色按下态 |
| `disabled` | `#E8E8E8` | 禁用态底色 |
| `onDisabled` | `#888888` | 禁用态文字 |
| `mutedText` | `#666666` | 次要文字 |
| `surface` / `text` | 同系列 | 通用表面与正文 |
| metrics | `badgeSize`、`compactControlHeight`、`controlHeight`、`navigationItemHeight` | 度量令牌 |
| shape | `controlRadius`、`pillRadius`、`surfaceRadius` | 三种圆角令牌 |

> ⚠️ **与本项目当前实现的差异**：本项目当前是「白色卡片 + `#E3E8F7` 描边」；ChatGPT 是
> **`#F3F3F3` 灰卡片 + `#DDDDDD` 结构线、卡片靠底色分层而非描边**。两者都成立，取舍见第六章。

### 4.5 `@SettingsAppearance` 注解机制（🔒 确定）

`SettingsTheme` 中把 `@SettingsAppearance` 标注在控件上，例如开关：

```
SettingsAppearance.toggleColors.checkedTrack / switchCheckedTrackColor
                                  / switchTrackColor / switchThumbColor
SettingsAppearance toggleDimensions
resolveSettingsTheme / isNativeIOS
toggleRowVerticalInset / toggleLabelVerticalInset
rowMinimumHeight / rowLabelGap / rowDisabledOpacity
accessibilityCategory.checkbox
OAIAlign.Center / OAIJustify.Between
```

**设计含义**：组件**只声明"我在设置页里属于哪类控件"**，具体颜色/尺寸由主题层统一解析注入。
这是它换主题后全站一致的根本原因，也避免了每个组件各自读色值造成的漂移。

---

## 五、DIL：智能界面引擎

聊天回复过程中动态出现的界面（表格、图表、卡片、表单等）由 **DIL** 驱动。

### 5.1 模块清单（🔒 确定）

| 模块 | 压缩后 / 解压后 | 职责 |
|---|---|---|
| `dil_core` | 56 KB / 239 KB | 基础：`DILStyle`、`DILLayout`、`DILColor`、`DILDynamicFont`、`DILSystemFontWeight` |
| `dil_compiler` | 28 KB / 105 KB | **编译**：`DILWarningRenderState`、`DILRenderChildState`、`DILTableCellRenderState`、`DILAutoSpacingRenderState`、`DILRenderChildCounter` |
| `dil_renderer` | 34 KB / 126 KB | 渲染器（TS 侧） |
| `dil_sandbox` | 51 KB / 213 KB | **沙箱求值** |
| `dil_generated_components` | 195 KB / **5.2 MB** | 预生成组件集 |
| `dil_cel` + `cel` | 17 KB + 15 KB | **CEL**（Common Expression Language）表达式 |
| `dil_charts` | 37 KB | 图表 |
| `dil_text` | 43 KB | 文本处理 |
| `dil_math` | 2 KB | 数学 |
| `dil_map` | 3 项（`assets/dil_map`） | 地图 |
| `dil_icon_assets` | 426 KB | 图标资源 |

Android 原生侧（在 dex 中确认存在类名）：**`DilRenderer` / `DilRendererImpl` / `DebuggableDilRenderer`**。

### 5.2 DIL 样式是 CSS 子集（🔒 确定）

`dil_core.js` 中列出的受支持样式属性：

```
alignItems alignSelf background backgroundColor bottom borderRadius boxSizing boxShadow
cursor display flex flexShrink fontSize height justifyContent left lineHeight
marginBlockEnd marginBlockStart marginBottom marginInlineEnd marginInlineStart
marginLeft marginRight marginTop overflow overflowX overflowY opacity pointerEvents
position right scrollbarWidth top transform transformOrigin width zIndex
```

→ 说明 DIL 采用**类 CSS 的声明式样式**，服务端（模型）可以用接近 Web 的方式描述界面。

### 5.3 渲染机制：帧栈 + 自动间距（🔒 确定）

`dil_core.js` / `dil_compiler.js` 中的核心符号：

```
# 渲染状态帧栈
renderState.pushFrame / popFrame / insertInRenderState / fromRenderState

# 文本样式继承
renderStateWithDILInheritedTextStyle / withDILInheritedTextStyleOverride
getDILInheritedTextStyle / getActiveDILInheritedTextStyleOverride

# V2 自动间距（AutoSpacing）
runWithDILV2ResponseAutoSpacing / beginDILV2AutoSpacingElement / endDILV2AutoSpacingElement
resolveDILV2ChildRenderOptions / resolveDILV2ChildRenderFrame
DIL_DEFAULT_CONTAINER_GAP
resolveDILSpacingUnits / resolveDILAutoSpacingChildMargin / disableAutoSpacing
DILRenderChildCounter

# 布局与对齐
renderStateWithDILFlexDirection / renderStateWithDILFlexibleParent / renderStateWithDILFlexAlignment
resolveDILAspectRatio / getDILStyleOverflowAxes / resolveDILStyleLineHeightRatio
```

**关键设计：V2 自动间距（AutoSpacing）** —— 容器与孩子之间的间距由引擎按规则计算
（`DIL_DEFAULT_CONTAINER_GAP`、`previousName`、`counter` 等参与），因此**模型生成界面时不需要
自己算间距**，排版由引擎兜底。这正是"智能界面"能自动成型的原因。

### 5.4 排版体系采用 iOS 风格命名（🔒 确定）

```
body callout caption1 caption2 footnote headline largeTitle subheadline title1 title2 title3
Normal / Medium / Semibold / Bold
SYSTEM_FONT_NAME / SYSTEM_MEDIUM_FONT_NAME / SYSTEM_SEMIBOLD_FONT_NAME / SYSTEM_BOLD_FONT_NAME
DILDynamicFont / DILDynamicTextStyle / resolveDILDynamicTextStyleForFontSize
DILSystemFontWeight / DILSystemFontForSizeCache / resolveDILStyleLineHeightPixels
```

→ 文本样式命名与 iOS Dynamic Type 一致（💡 推断：为跨平台视觉统一而采用 Apple 的命名体系）。

### 5.5 容错：属性失败不中断渲染（🔒 确定）

```
notifyDILWarningReporter / DILWarningReporter / DILWarningRenderState
"Failed to apply DIL style property \"...\""      ← 单属性失败仅上报
```

**这是运行时安全的关键设计**：服务端下发的界面描述一旦含有未知属性，只降级并上报，
**不会导致整个界面崩溃**。

### 5.6 推定的完整链路（🔍 确证 + 💡 推断）

```
服务端返回 DIL 描述（类 CSS 样式 + 结构 + 数据绑定 + CEL 表达式）
      ↓
dil_sandbox      沙箱求值（隔离不可信内容）
      ↓
dil_compiler     RenderState 帧栈编译：样式继承 / AutoSpacing / 表格 / 布局方向
      ↓
dil_renderer     TS 侧生成渲染树
      ↓
DilRendererImpl  原生侧（Kotlin）映射为原生 View / 原生组件
      ↓
若某属性不可用 → DILWarningReporter 上报，界面继续渲染
```

> 其中「原生侧如何把渲染树映射为具体控件」属 💡 推断 —— 依据是原生类名
> `DilRendererImpl` 与 `assets/valdi_attributes_binders/com.openai.valdi.dil.widgets.*` 的存在，
> 但**未反编译其方法体**（见第八章）。

---

## 六、可借鉴的设计点

| # | ChatGPT 的做法 | 对本项目的意义 |
|---|---|---|
| 1 | **控件声明语义、主题层注入取值**（`@SettingsAppearance` + `resolveSettingsTheme`） | 本项目目前的 `SettingsGroup` / `ImmersiveTopBarIconButton` 各自读 `colorScheme`。设置项增多后建议收敛为一个"设置页主题解析"入口，避免色值分散 |
| 2 | **卡片靠底色分层，而非描边**（`groupedSurface #F3F3F3` + `border #DDDDDD`） | 本项目当前是「白卡 + 描边」。它的方案在深色模式下**无需切换描边色**；若采用，需把"结构线"与"卡片底色"分成两个角色 |
| 3 | **动态内容必须具备渲染容错**（单属性失败只告警） | 若未来接入任何"服务端下发 UI"，必须做同样的降级，不能整页崩溃 |
| 4 | **动态内容在沙箱中求值**（`dil_sandbox`） | 不可信描述不得直接进入渲染上下文 |
| 5 | **排版兜底交给引擎**（AutoSpacing） | 生成式 UI 不能依赖模型自己算间距 |
| 6 | **菜单用平台原生控件**（`NativeMenu` + `onFallbackPress`） | 系统菜单天然具备正确的平台交互与无障碍；跨平台时保留回退实现 |

---

## 七、复现步骤与工具链

### 7.1 本机工具链

| 工具 | 路径 |
|---|---|
| JDK 21 | `D:\ReverseTools\jdk21` |
| jadx | `D:\ReverseTools\jadx\bin\jadx.bat` |
| apktool | `D:\ReverseTools\apktool\apktool.bat` |
| Android SDK / aapt2 | `D:\AndroidDev\android-sdk`（build-tools 34.0.0） |
| Python | 3.14（**标准库自带 `compression.zstd`**） |

### 7.2 关键步骤

**① 基础信息**

```powershell
aapt2 dump badging <apk> | Select-String "^package|sdkVersion|launchable-activity"
```

**② 识别 Valdi 模块并解包**

`.valdimodule` 文件头为 `28 B5 2F FD`（Zstandard）。用 Python 3.14 直接解压：

```python
from compression import zstd
import pathlib

raw = pathlib.Path("dil_core.valdimodule").read_bytes()
pathlib.Path("dil_core.js").write_bytes(zstd.decompress(raw))
```

**③ 从解压后的 JS 中提取符号上下文**

由于打包产物是单行长文本，需按关键词截取上下文（建议输出到 UTF-8 文件，避免 Windows
控制台 GBK 编码错误）：

```python
import pathlib, re
data = pathlib.Path("oai_ui.js").read_text(errors="replace")
for m in re.finditer(re.escape("SettingsMenuRow"), data):
    print(data[max(0, m.start()-200): m.end()+700])
```

**④ 在 dex 中检索类名**

```python
import pathlib, re
data = pathlib.Path("classes5.dex").read_bytes()
strings = re.findall(rb"[\x20-\x7e]{6,}", data)   # 定位 DilRendererImpl 等
```

---

## 八、本次分析的边界

### 已完成（可复现）

- 包信息与 SDK 声明、APK 结构与模块清单
- `.valdimodule` 格式识别（zstd）与 **9 个关键模块的解包**
- 从**未混淆**的 JS 源码中取得主题色值、组件符号、DIL 机制符号
- dex 字符串检索定位到 `DilRendererImpl`、`PersonalizationSettingsScreen` 等类名

### 未完成

| 项 | 原因 |
|---|---|
| Android 侧 `DilRendererImpl` 的实现细节 | 未做 jadx 全量反编译（8 dex / 67 MB，约需 10–20 分钟）。**因此原生渲染映射结论属 💡 推断** |
| `dil_generated_components`（解压 5.2 MB）的组件清单 | 尚未系统性梳理 |
| REA 的 Android 分析工具 | 需要 `REA_JADX_MCP_JAR` 与 `REA_APKTOOL_COMMAND` 两个环境变量（见下） |
| 主题切换后的状态持久化链路 | 未追踪（涉及 `PersonalizationSettingsRepository` 实现体） |

### REA 工具配置状态

REA MCP 已注册在 `~/.codebuddy/mcp.json`，但其两个 Android 能力需要环境变量：

| 变量 | 需要指向 | 本机状态 |
|---|---|---|
| `REA_APKTOOL_COMMAND` | apktool 启动器 | ✅ `D:\ReverseTools\apktool\apktool.bat` 可用 |
| `REA_JADX_MCP_JAR` | `jadx-headless-mcp` 的 JAR | ⚠️ 待补 |

配置方式：在 `mcp.json` 的 `REA` 条目加入 `env`，然后**重启 IDE**。

---

## 九、图标体系：自研为主

### 9.1 主体是 OpenAI 自研的设计系统（🔒 确定）

UI 图标（设置页行图标、聊天界面图标等）位于：

```
assets/composeResources/com.openai.kmp.platform.design.resources/drawable/
```

共 **513 个**以 `ds_`（design system）为前缀的矢量图标。

**决定性证据** —— 每个图标文件内部都带有生成标记（原文）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/sync-design-system. Do not edit. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
```

即这些图标由 OpenAI **自己的设计系统同步脚本**从内部设计源生成，**不是从开源图标库拷贝**。

**命名规范**（与 Apple SF Symbols 同构）：

| 维度 | 取值 | 分布 |
|---|---|---|
| 字重 | `regular` / `light` / `bold` / `fill` | `regular_24`：452 个 |
| 尺寸 | 16 / 20 / 24 / 28 | `light_28`：12、`dark_28`：8、`light_24`：7、`regular_16`：5… |

示例：`ds_american_football_regular_24`、`ds_alarm_clock_checkmark_fill_regular_24`、
`ds_arrow_curvedpath_left_lg_regular_24`、`ds_agent_badge_plus_regular_24`。

> 💡 **推断**：命名与视觉语言对齐 **Apple SF Symbols**（`isNativeIOS` 分支、iOS 风格排版命名
> `body/callout/title1` 同为旁证），但**图标本体为自绘** —— SF Symbols 是 Apple 专有资产，
> 无法在 Android 上合法使用。

### 9.2 少量开源图标：Lucide（🔍 确证，其中一处存疑）

`dil_icon_assets` 模块（品牌 logo 索引）中，每条记录形如：

```
appleFruit / apple / apple-logo / lucide
facebook  / facebook / facebook-logo / lucide
github    / github / github-logo / lucide
instagram / instagram / instagram-logo / lucide
slack     / slack / slack-logo / lucide
youtube   / youtube / youtube-logo
```

- 图标集标识：**lucide**（10 处）
- 上游：**https://github.com/lucide-icons/lucide**（ISC 许可，1600+ SVG，Feather Icons 的 fork）
- 图标本体**不在 APK 内** —— `dil_icon_assets.valdimodule` 只是索引（解压后 494 KB，引
  `valdi_core/src/AssetCatalog` + `loadCatalog` + `res.assetpackage`），未能直接比对 SVG 路径

> ⚠️ **存疑点**：Lucide 官方 README **明确不接受品牌图标**
> （"Lucide does not accept brand logos, and we do not plan to add them in the future"）。
> 这些品牌 logo 可能来自早期含品牌图标的 Feather/Lucide 版本，或为 OpenAI 自绘后沿用该标记。
> **此点未能证实。**

### 9.3 其他自研矢量资源（🔒 确定）

```
assets/openai/beacon/final_composition_icon.svg        1,700 B
assets/openai/beacon/final_composition_icon_dark.svg   1,700 B
assets/openai/imagegen/notification_bell.svg
assets/openai/imagegen/notification_bell_dark.svg
```

### 9.4 字体（🔒 确定）

| 字体 | 文件 | 说明 |
|---|---|---|
| Söhne | `soehne_circle.otf` / `soehne_kraeftig.ttf` | ChatGPT 品牌字体（Klim Type Foundry 商业字体） |
| Menlo | `res/font/menlo_bold.ttf` / `menlo_regular.ttf` | 等宽字体（源自 Bitstream Vera，开源） |
| Roboto Medium Numbers | `res/font/roboto_medium_numbers.ttf` | Android 系统字体（Apache-2.0） |

---

## 十、DSL 引擎的来源：自研，构建在开源 Valdi 之上

### 10.1 DIL 本身是 OpenAI 自研（🔒 确定）

- 模块：`dil_core` / `dil_compiler` / `dil_renderer` / `dil_sandbox` / `dil_cel` / `dil_widgets` /
  `dil_charts` / `dil_text` / `dil_math` / `dil_map` / `dil_generated_components`
- 原生侧命名空间：`com.openai.valdi.dil.widgets.*`
- 在 84 个 Valdi 模块中，`dil_*` 是一组自成体系的模块，不属于 Valdi 官方组件集

### 10.2 核心框架：Valdi（Snapchat 开源）（🔒 确定）

| 项 | 值 |
|---|---|
| 名称 | **Valdi** |
| **地址** | **https://github.com/Snapchat/Valdi** |
| 许可证 | MIT |
| 热度 | 约 16.4k stars |
| 定位 | 跨平台 UI 框架；TypeScript 声明式编写，编译为 iOS/Android/macOS **原生视图**；官方说明 "no web views, no JavaScript bridges"，自带 C++ 布局引擎 |
| 状态 | Beta（已在 Snap 生产环境使用 8 年） |

**证据**：84 个模块**全部**引用 `Valdi`（`valdi_core.js` 130 次、`dil_widgets.js` 84 次、
`valdi_ui.js` 64 次）；原生模块 `com.snap.valdi.modules.*`；属性绑定器
`com.snap.valdi.modules.webview.ValdiWebViewAttributesBinder`；`assets/valdi_api_version`。

### 10.3 表达式语言：概念源自 CEL，实现自研（🔍 确证）

| 项 | 值 |
|---|---|
| 名称 | CEL（**Common Expression Language**） |
| **规范地址** | **https://github.com/google/cel-spec**（现属 `cel-expr` 组织） |
| 许可证 | Apache-2.0 |
| 官方定位 | 表达式求值的通用语义；无副作用、非图灵完备、线性时间求值 |

`dil_cel.js` 反解出的 AST 节点（原文）：

```
ExpressionKind: Identifier / Member / Index / Literal
Unary:  operator.UnaryOperator.Negate / Not / argument
Binary: left / right
Conditional: condition / whenTrue / whenFalse
List:   items
Call:   key / args
CELParser.parse / compileFromAST / resolveValueAtPath / isCELMap
DILStateMutationForCELScope / DILWidgetBinding
```

> ⚠️ **重要区分**：上述结构与 **Google CEL 官方规范并不一致**。
> 官方 `syntax.proto` 采用 snake_case（`ident_expr` / `select_expr` / `call_expr` /
> `list_expr` / `struct_expr`），且 **Unary / Binary / Conditional 并非独立节点**，而是
> `call_expr` 的特例（运算符以函数名表达：`!_`、`_&&_`、`_?_:_`、`_[_]`）。
>
> 结论：**DIL 的 `CELParser` 是自研实现**，概念上借鉴 CEL（受限求值、无副作用），
> AST 与规范无关 —— 这也符合它需运行在 Valdi 的 JS runtime、无法直接使用 `cel-go`/`cel-java` 的现实。

### 10.4 其他开源依赖（🔒 确定）

| 组件 | 证据 | 上游地址 | 许可 |
|---|---|---|---|
| **Mapbox GL JS**（地图） | `assets/dil_map/mapbox.js`、`mapbox.html`、`mapbox-valdi-bridge.js`；`dil_widgets.js` 含 `mapbox`×3 | `https://github.com/mapbox/mapbox-gl-js` | v1 为 BSD-3；v2+ 为 Mapbox 专有条款 |
| **JAX**（数值计算） | `math_jax.valdimodule`（571 KB），`jax` 出现 727 次 | `https://github.com/jax-ml/jax` | Apache-2.0 |
| **SQLite**（本地 SQL） | `client_sql.js` 含 `sqlite`×4 | `https://sqlite.org` | Public Domain |
| **Lucide**（图标） | `dil_icon_assets.js` 含 `lucide`×10 | `https://github.com/lucide-icons/lucide` | ISC |
| **Compose Multiplatform** | `assets/composeResources/`（1087 项），命名空间 `com.openai.kmp.platform.*` | `https://github.com/JetBrains/compose-multiplatform` | Apache-2.0 |

> `orbit-*` 系列资源（3D 资产，约 760 项）附带 `assets/orbit-licenses/`，内含 Android NDK、
> LLVM、Khronos、openlibm 的许可声明，属其渲染运行时的依赖。

---

## 十一、两处具体图标的追查

### 11.1 设置页菜单行的「倒三角」：自研矢量图标，非位图（🔒 确定）

**结论：不是图片，也不是 emoji，而是 Valdi 自研的矢量图标（存于 AssetCatalog）+ 运行时旋转。**

Valdi 侧的图标资源名（注意是 `oai_` 前缀，与 Compose 侧的 `ds_` 不同）：

```
oai_chevron_up_down_system   ← 上下双箭头（菜单的展开/收起指示器）
oai_chevron_right_system     ← 右箭头
oai_chevron_right_sm         ← 右箭头（小号）
oai_check_md                 ← 勾选
```

`oai_ui.js` 中菜单行相关的符号（原文）：

```
disclosure, oaiChevronUpDownSystem        ← 菜单的 disclosure 指示器
  .src / .tint / .translationX
disclosure$ .rotation.PI * oaiChevronRightSystem.IconPrimary(IconSystemDisclosure
  .size / .disclosureSize / .menuDefaultTextHeight
showsChevron / menuIndicator / menuAccessory / menuValue / stackedMenuContent
IconSystemDisclosure / navigationDisclosure / navigationAccessory
```

要点：

1. 图标本体是 **AssetCatalog 中的矢量资源**（`valdi_core/src/AssetCatalog` + `loadCatalog`），
   **不是 PNG/WebP 位图，也不是 emoji**；
2. 指示器有两种实现——直接用 **`oaiChevronUpDownSystem`**（原生上下双箭头），
   或把 **`oaiChevronRightSystem` 经 `.rotation`（PI = 180°）旋转**成向下箭头；
3. 尺寸由主题令牌控制：`disclosureSize` / `menuIndicatorWidth` / `menuIndicatorHeight` /
   `menuDefaultTextHeight`；
4. 颜色走 `.tint`，配合 `resolveValdiUITheme` 的主题色。

**对应的 Android 侧资源**（Compose 用的 513 个 `ds_` 图标之一）：

```
ds_chevron_down_md_regular_24
ds_chevron_down_sm_regular_24
ds_chevron_up_chevron_down_sm_regular_24     ← 上下双箭头版本
ds_chevron_up_md_regular_24
ds_chevron_right_md_regular_24 / _lg_ / _sm_
```

> ChatGPT 同时存在两套图标命名空间：`oai_*`（Valdi 用，仅 4 个符号）与 `ds_*`（Compose 用，
> 513 个 UI 图标）。设置页由 Valdi 渲染，因此实际使用的是前者。

### 11.2 语音模式的「模型按钮」图标（🔒 确定 + 一处待运行时确认）

**结论：语音模式界面由服务端下发的 DIL 生成，因此该按钮的图标不由客户端静态代码决定。**

**证据一：语音模式卡片是生成式 UI**（`dil_generated_components.js` 原文节选）

```javascript
// src/restricted/components/LearningVoiceModeLauncherCard.generated.js
createComponent) => {
    const hostGenUI = GenUI;                          // GenUI = Generative UI
    const unscopedComponent = createComponent(DIL, hostGenUI);
    function ResolvedComponentLearningVoiceModeLauncherCard(props) {
      const resolutionId = props.__resolutionId;
      const Component = DIL.useMemo(() => {
        if (typeof resolutionId !== "string") return unscopedComponent;
        const widgetGenUI = Object.freeze({
          ...hostGenUI,
          refresh: hostGenUI.refreshWidget ? () => hostGenUI.refreshWidget({
            nativeWidgetResolutionId: resolutionId,
            componentName: "LearningVoiceModeLauncherCard",
          }) : undefined,
          dispatchAction(action) {
            ...
            if (action.handler !== "server" && !isBoundShare) return hostGenUI.dispatchAction(action);
            return hostGenUI.dispatchWidgetAction({ ... });
          },
        });
```

即语音模式的启动卡片是**服务端生成的组件**（`*.generated.js` + `GenUI` + `resolutionId`），
界面描述（含图标）来自服务端，交互动作也回传服务端处理（`action.handler === "server"`）。

**证据二：模型清单与服务端接口**（`chatgpt_services.js` 原文）

```
GET  /backend-api/models
POST /backend-api/sentinel/chat-requirements
ModelCatalogRequest / ModelCatalogModel / ModelCatalogPreset / ModelCatalogVersion / ModelCatalogResponse
intelligence_presets / model_slug / default_model_slug
supports_model_picker_upgrade_presets
"ChatGPT did not return an available default model."
"The selected model \"…\" is not available to this account."
workmode.Standard / sol / terra                ← 模型代号
```

**证据三：模型可以直接「写出 DIL」**（同文件）

```
decodeModelWrittenDILResult      ← 模型输出的 DIL 结果被解码
```

> 这是 Intelligent UI 的关键闭环：**模型输出 DIL 描述 → 客户端解码 → 渲染为原生界面**。

**客户端可供 DIL 选用的语音类图标**（513 个 `ds_` 图标中的相关项）：

```
ds_voice_regular_24 / ds_voice_button_regular_24
ds_microphone_regular_24 / _fill_ / _slash_
ds_wavelength_regular_24
ds_speaker_sound_wave_2_regular_24 / _fill_
ds_headphones_regular_24
ds_person_and_wavelength_regular_24
ds_chat_bubble_and_wavelength_regular_24
ds_page_text_and_microphone_regular_24
ds_video_badge_wavelength_regular_24
ds_screen_wave_regular_24
```

> ⚠️ **待确认**：具体是上述哪一个（或服务端传入的其他图标 id）**无法由静态代码判定**，
> 它取决于服务端下发的 DIL 描述。要确证需要**运行时取证**：在设备上展开语音模式并抓取
> 服务端下发的 DIL 内容，或 hook DIL / AssetCatalog 的图标解析。

---

## 十二、DIL 引擎的完整架构（深挖）

### 12.1 模块分层（解压后实测）

`dil_widgets` 解压后 1.62 MB，包含 **228 个模块**，按职责分层：

| 层 | 模块数 | 代表模块 |
|---|---|---|
| `widgets/` | **113** | `Box` / `Col` / `Row` / `Grid` / `Flow` / `Inline` / `Card` / `Button` / `Divider` / `Icon` / `Image` / `Text` …（每个都有配对的 `*WidgetRenderer`） |
| `elements_renderer/` | **40** | 见 12.2 |
| `webview/` | 7 | `WebWidget`、`WebWidgetRenderer`、`WebWidgetSizing`、`StateBackedWebWidgetNode`、`WebViewDocument` |
| `maps/` | 3 | `MapSurface`、`NativeMapWebViewSurface`、`MapTypes` |
| `popover/` | 3 | `PopoverTrigger`、`PopoverContent`、`pointerIntent` |
| `layout/` | 3+ | `TableLayout`、`GridLayoutAttributes`、`FlowPlacement` |
| 其它 | — | `animation/`（`DILTransitionDefinition`、`KeyFrameAnimator`、`FlashcardFlip`）、`colors/`、`typography/`、`text_elements/`、`rendering/`、`style/`、`utils/` |

### 12.2 支持的 DIL 元素全集（40 个渲染器）

**这是 DIL 能表达的 UI 元素清单**（🔒 确定，取自 `elements_renderer/`）：

| 类别 | 元素 |
|---|---|
| **布局** | `Box` · `FlowItem` · `List` · `Table` · `Transparent` · `Component` · `ComponentContent` |
| **文本** | `Text` · `Markdown` · `TextOnlyMarkdown` · `ShimmerText` · `InlineTextStyle` |
| **基础控件** | `Button` · `Checkbox` · `Radio` · `Choice` · `Pressable` · `Form` · `Badge` |
| **媒体** | `Image` · `Svg` · `Lottie` · `YouTube` · `Carousel` |
| **富内容** | `Math`（数学公式）· `Blockquote` · `Icon` |
| **浮层** | `Popover` · `Tooltip` |
| **动态** | `Animate`（+ `AnimateGroup` / `AnimateItem`）· `Transition` · `FlashcardFlip` |
| **扩展** | `ClientDefinedWidget`（客户端自定义组件）· `RenderedProp` |
| **辅助** | `DILPropNameValidation`（属性名校验）· `TransitionAnimationOptions` · `primitiveChildrenToString` |

另有 `DILCardElementRenderer`、`DILCarouselItemElementRenderer`、`DILCarouselMediaItemElementRenderer` 等派生元素。

### 12.3 组件体系：288 个预置组件（🔒 确定）

`dil_generated_components` 解压后 **5.25 MB**，含 **227 个已解析组件 + 288 个 `*.generated.js` 文件**。按业务域分类：

| 业务域 | 组件（举例） | 数量 |
|---|---|---|
| **临床评分计算器** | `CalculatorGlasgowComaScaleGcsWidget`、`CalculatorApgarScoreWidget`、`CalculatorNihStrokeScaleNihssWidget`、`CalculatorCha2ds2VascScore*`、`CalculatorPaduaPredictionScore*`、`CalculatorOttawaAnkleRuleWidget`、`CalculatorPecarnPediatricHeadInjuryRuleWidget` … | **49** |
| **`*Widget` 卡片** | 见下 | **100** |
| **索取用户信息** | `AskUserDetails`、`AskUserFiles`、`AskUserLocation`、`AskUserLocationV2`、`AskUserLocationV2Bidi` | 5 |
| **学习（Learning）** | `LearningQuiz`、`LearningFlashcards`、`LearningVocabCard`、`LearningFillBlankCard`、`LearningSpeakCard`、`LearningImageChoiceCard`、`LearningSentenceBuilderCard`、`LearningAudioLabelCard`、`LearningVoiceModeLauncherCard` | 9 |
| **追问（FollowUp）** | `FollowUpCardGroup`、`FollowUpChoiceCard`、`FollowUpLinkList`、`FollowUpPillGroup`、`FollowUpQuiz`、`ConversationFollowUpActionList` | 6 |
| **对话式引导** | `ConversationalOnboardingAdvice` / `FollowUpPills` / `Search` / `Study` / `Writing` | 5 |
| **记账（Ledger）** | `LedgerCreditScoreDetail`、`LedgerSpendByCategoryDetail`、`LedgerTransactionDetail`、`LedgerUpcomingActivityRecentTransactions`、`LedgerLowConfidenceCategoryCorrection` | 5 |
| **体育赛事** | NBA / NFL / MLB / NHL / WNBA / NCAA / 足球 / 网球 / 板球 / F1 / 疯狂三月，多为 `*ScheduleWidgetWithSource` 与 `*StandingsWidgetWithSource` | ~30 |
| **天气 / 金融** | `WeatherWidgetV1/V3/WithSource`、`WeatherCurrentWithSource`、`StockChart`、`StockComparisonChart`、`StockHeatmap` | 8 |
| **出行 / 购物 / 邮件** | `FlightSearchForm`、`FlightResults`、`ProductCarousel`、`PackageTracker`、`DraftEmail`、`EmailPreview` | 6 |
| **实体 / 检索** | `EntityCard`、`EntityExplorer`、`EntityThumbnailList`、`NewsArticleWidgetCarousel`、`SidebarPeopleAlsoAsk`、`SidebarFactTable`、`WebLinksCarousel`、`Reddit*` | 10 |
| **任务 / 自动化** | `TaskAutopauseCard`、`TaskExpansionCard`、`PromptChecklist`、`SuggestAutomation`、`ListAutomations`、`PluginFollowup` | 6 |
| **持久化推送（Pulse）** | `PersistedWidgetDogfood*`（Goal / ExerciseLog / DailyNutrition / Checklist）、`PulseDraftEmail`、`PulseExpansionCard`、`PushDripSeriesIntro`、`StopPushDripSeries` | 8 |
| **系统卡片** | `Gpt4oSystemCard`、`Gpt5SystemCard`、`Onboarding*`、`WhatsNewCapabilityWelcome` | 6 |

### 12.4 组件来源分档与懒加载（🔒 确定）

`DILComponentSources.generated.js` 原文：

```javascript
DIL_COMPONENT_SOURCES = {
    ...DILPublicComponentSources,
    ...restricted/DILRestrictedComponentSources,   // ← 受限组件源
}.freeze();
```

配套 `DILComponentNames.generated.js` 导出：
- **`DIL_COMPONENT_NAMES`** —— 全部组件名
- **`DIL_LAZY_COMPONENT_NAMES`** —— **懒加载组件名**（按需解析，不一次性实例化）

即：组件按 **公开 / 受限** 两档授权，并按 **懒加载** 清单按需载入。

### 12.5 安全与健壮性设计（🔒 确定）

`dil_widgets` 的基础设施层暴露了一整套防护：

| 机制 | 模块 | 作用 |
|---|---|---|
| **沙箱** | `dil_sandbox`、`DILSandboxTraceEnvelope`、`DILSandboxTraceReporting`、`DILSandboxDownloadURL` | 隔离执行、追踪、限制下载 |
| **网络策略** | **`DILTrustedNetworkPolicy`**、`DILTrustedNetworkRenderState` | 只允许可信来源 |
| **渲染健康度** | **`DILRenderHealthMetrics`**、`DILRenderOutputInspection`、`DILRenderOutputMonitor` | 监控渲染输出是否异常 |
| **容错** | `DILErrorBoundary`、`DILErrorReporting`、`DILInlineError` | 局部失败不扩散 |
| **属性校验** | `DILPropNameValidation` | 未知属性只告警（对应 "Failed to apply DIL style property"） |
| **动作权限** | `DILActions`、`DILClientActionCapabilities`、`DILHostActionsHandler` | 区分客户端/服务端动作与能力授权 |

### 12.6 完整链路（🔍 确证）

```
服务端 / 模型生成 DIL 描述（类 CSS 样式 + 元素树 + 数据绑定 + CEL 表达式）
      │
      ├─ dil_sandbox      沙箱求值（可信网络策略、下载限制、追踪上报）
      ├─ dil_cel          CEL 表达式求值（绑定路径、条件、列表）
      ├─ dil_compiler     RenderState 帧栈编译：样式继承、AutoSpacing、表格
      │                   （`DILFlexContainerAutoSpacing` 负责自动间距）
      ▼
  dil_widgets  40 个元素渲染器 + 113 个 widget + 布局/动画/颜色/排版
      │            └─ WidgetNode → Valdi → 原生视图
      ├─ dil_charts（ChartWidgetRenderer 原生 + ChartWebWidgetRenderer 网页双实现）
      ├─ dil_map   （MapSurface / NativeMapWebViewSurface → WebView + Mapbox GL JS）
      ├─ webview   （WebWidget / StateBackedWebWidgetNode）
      └─ lottie / svg / youtube 元素
      ▼
  出错 → DILErrorBoundary + DILWarningReporter（单点失败不中断整页）
```

---

## 十三、智能界面的运行时协议（深挖）

### 13.1 组件用 TypeScript 编写（🔒 确定）

`*.generated.js` 内部保留了源码分区标记：

```javascript
//#region shared.dil.ts
	function useResolvedComponentResult(componentName, props) { … }
```

即 DIL 组件以 **TypeScript（`.dil.ts`）**编写，编译后产出 `*.generated.js`。

### 13.2 两阶段运行时：服务端解析 → 客户端渲染（🔒 确定）

每个生成组件都有一个**外层包装**，它与内层实现成对出现：

```javascript
(createComponent) => {
    const hostGenUI = GenUI;                                    // GenUI = Generative UI
    const unscopedComponent = createComponent(DIL, hostGenUI);   // 内层：本地默认实现
    function ResolvedComponentAskUserLocationV2Bidi(props) {
      const resolutionId = props.__resolutionId;                 // ★ 服务端解析 ID
      const Component = DIL.useMemo(() => {
        if (typeof resolutionId !== "string") return unscopedComponent;   // ★ 降级：用本地实现
        const widgetGenUI = Object.freeze({
          ...hostGenUI,
          refresh: … refreshWidget({ nativeWidgetResolutionId: resolutionId, componentName }),
          dispatchAction(action) {
            const isBoundShare = action.handler === "client" && action.type === "share" && …;
            if (action.handler !== "server" && !isBoundShare) return hostGenUI.dispatchAction(action);
            return hostGenUI.dispatchWidgetAction({                 // ★ 服务端动作回传
              type: action.type, payload: action.payload,
              nativeWidgetResolutionId: resolutionId, componentName,
            });
          },
        });
        const widgetDIL = Object.freeze({
          ...DIL,
          useState(initialState, options) {
            return DIL.useState(initialState, { ...options, widgetId: resolutionId });   // ★ 状态隔离
          },
        });
        return createComponent(widgetDIL, widgetGenUI);
      }, [resolutionId]);
      return __dil.jsx(OpGenuiResolvedComponentBoundary, { componentName, componentProps: props, Component });
    }
    return Object.assign(ResolvedComponent…, unscopedComponent);
}
```

要点：
1. **`__resolutionId`** 标识"这次渲染对应服务端的一次解析"；
2. **无 `resolutionId` 时降级**到 `unscopedComponent`（本地默认实现）→ **没有服务端数据也能渲染**；
3. **状态按 widget 隔离**：`useState` 自动带上 `widgetId: resolutionId`，同一组件的多个实例互不干扰；
4. **动作分流**：`handler === "server"` 的动作经 `dispatchWidgetAction` 回传服务端，其余本地处理；
5. `OpGenuiResolvedComponentBoundary` 作为边界，隔离单个组件的失败。

### 13.3 解析状态机与错误码（🔒 确定）

```javascript
function useResolvedComponentResult(componentName, props) {
    const opGenui = DIL.useAppData((appData) => appData.opGenui);
    if (props.__state !== void 0) return { status: "resolved", resolutionComplete: true, state: props.__state };

    const key = componentDataKey(componentName, props);   // JSON([componentName, normalizeComponentProps(props)])
    const result = opGenui?.componentResults?.[props.__resolutionId] ?? opGenui?.componentResults?.[key];

    if (isComponentResultEnvelope(result)) {
        if (result.status === "pending")  return { status: "pending",  resolutionComplete: false, state: void 0 };
        return result.status === "resolved"
            ? { status: "resolved", resolutionComplete: true,  state: result.state }
            : { status: "failed",   resolutionComplete: true,  state: void 0 };
    }

    const legacyState = opGenui?.componentData?.[key];    // ④ 兼容旧协议
    if (legacyState !== void 0) return { status: "resolved", resolutionComplete: true, state: legacyState };
    …
    return { status: "pending", resolutionComplete: false, state: void 0 };
}

function isComponentResultEnvelope(value) {
    …
    if (result.status === "pending")  return true;
    if (result.status === "resolved") return typeof result.state === "object" && result.state != null && !Array.isArray(result.state);
    return result.status === "failed"
        && (result.code === "component_unavailable"      // 组件不可用
         || result.code === "tool_failed"                // ★ 工具调用失败
         || result.code === "invalid_tool_result")       // ★ 工具结果无效
        && typeof result.retryable === "boolean";        // ★ 可重试标志
}
```

| 机制 | 说明 |
|---|---|
| **状态三态** | `pending`（解析中）→ `resolved`（成功）/ `failed`（失败） |
| **失败错误码** | `component_unavailable`、**`tool_failed`**、**`invalid_tool_result`** |
| **可重试** | `retryable: boolean` |
| **缓存键** | `JSON.stringify([componentName, 规范化后的 props])` |
| **数据落点** | `appData.opGenui.componentResults[resolutionId]` |
| **双协议兼容** | 新 `componentResults`（envelope）+ 旧 `componentData`（裸状态） |

### 13.4 关键结论：数据来自「服务端工具调用」（🔍 确证）

`tool_failed` / `invalid_tool_result` 这两个错误码说明：**智能组件的数据不是组件自己取的，而是模型声明工具调用、服务端执行后回填的**。完整闭环：

```
模型决定「用哪个组件 + 需要哪些数据」
      ↓ 声明工具调用
服务端执行工具（搜索 / 计算 / 第三方 API）
      ↓
componentResults[resolutionId] = { status: "resolved", state: {...} }
      ↓
客户端组件渲染 state（绑定通过 CEL 表达式求值）
      ↓ 组件内动作（重试 / 提交 / 分享）
dispatchWidgetAction → 服务端
```

### 13.5 CEL 表达式语言（🔒 确定）

**语法**（从 `cel.js` 解析器的报错信息反推）：

| 能力 | 证据 |
|---|---|
| 成员访问 | `Member`、`Cannot read field`、缺失字段告警 |
| 索引 | `Index`、`Invalid list index` |
| 函数调用 | `Call` / `args` / `Expected … after arguments` |
| 字面量 | `Literal`（字符串转义校验 `Invalid string escape`） |
| 列表 | `List` / `items` / `Expected … after list literal` |
| 映射 | `entries` / `Expected … after map key` / `Expected … after map literal` |
| 分组 | `Expected … after grouped expression` |
| 条件 | `Expected … after boolean condition`（三元 `_?_:_`） |
| 标识符/关键字 | `Identifier` / `Keyword` |

**`dil_cel` 的自研扩展**（🔒 确定，来自其内部函数与告警）：

| 扩展 | 作用 |
|---|---|
| **`state`** | 绑定表达式可读取组件状态 |
| **`localeConvertCurrency()`** | 货币换算（带汇率，失败时告警并降级） |
| `localeNumberToString()` / `localeStringToNumber()` | 本地化数字格式互转 |
| `round()` / `pow()` / `isFinite()` | 数学函数（`round` 支持精度；非法精度告警并降级） |
| **`emitWarning`** | 表达式内可上报渲染告警 |
| **`Invalid widget binding path`** | widget 绑定路径校验 |

> 注意：`dil_cel` 的 AST 命名（`ExpressionKind.Identifier/Member/Index/Literal`、`Unary/Binary/Conditional` 为独立节点）与 **Google CEL 官方规范不同**（后者用 snake_case，且 Unary/Binary/Conditional 只是 `call_expr` 的特例）—— 即**概念借鉴、实现自研**，详见第十章 10.3。

---

## 附：证据等级速查

| 结论 | 等级 |
|---|---|
| UI 由 Valdi 驱动、`.valdimodule` 为 zstd 格式 | 🔒 确定 |
| `SettingsMenuRow` 使用 `NativeMenu` 弹菜单、含 `onFallbackPress` | 🔒 确定 |
| 主题三层体系与默认色值（含 `#F3F3F3` / `#DDDDDD` / `#111111`） | 🔒 确定 |
| `@SettingsAppearance` 注解 + `resolveSettingsTheme` 注入 | 🔒 确定 |
| DIL 的模块划分、CSS 子集属性、AutoSpacing、容错告警 | 🔒 确定 |
| DIL 完整链路（沙箱 → 编译 → 渲染 → 原生映射） | 🔍 确证 + 💡 推断（末段） |
| Compose 与 Valdi 并存的原因（迁移中或分工） | 💡 推断 |
