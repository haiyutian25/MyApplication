# ChatGPT DIL 的线格式与渲染侧归属

回答两个问题：

1. **模型要「回复什么格式」**，引擎才能把组件组合成 UI 展示？
2. **渲染发生在哪一侧** —— 服务端渲染好再回传，还是客户端本地渲染？

- 分析日期：2026-10-12
- 目标文件：`com.openai.chatgpt_1791789377928.apk`
- 目标版本：`versionName 1.2026.279` / `versionCode 2627909`
- 分析方法：静态分析（**未运行**该 APK，未抓包）
- 本文是 [`CHATGPT_DIL_CLIENT_ACTIONS.md`](./CHATGPT_DIL_CLIENT_ACTIONS.md) 的姊妹篇：那一篇讲**客户端被授权做什么动作**，本篇讲**服务端要下发什么、以及在哪渲染**

## 目录

- [一、两个问题的答案](#一两个问题的答案)
- [二、方法与证据等级](#二方法与证据等级)
- [三、投递链路：模型 → 服务端 → 客户端](#三投递链路模型--服务端--客户端)
- [四、DIL 文档格式规范](#四dil-文档格式规范)
- [五、渲染侧归属：本地渲染](#五渲染侧归属本地渲染)
- [六、混合例外：WebView 与远端 HTML](#六混合例外webview-与远端-html)
- [七、不可信代码的本地沙箱](#七不可信代码的本地沙箱)
- [八、组件以「源码字符串」形式携带](#八组件以源码字符串形式携带)
- [九、复现步骤与证据清单](#九复现步骤与证据清单)
- [十、边界与未决问题](#十边界与未决问题)

---

## 一、两个问题的答案

### 结论速览

| 问题 | 答案 | 等级 |
|---|---|---|
| 渲染在哪一侧 | **客户端本地渲染**。服务端只给「界面描述 + 数据」，客户端自己 parse → compile → 渲染成 **Valdi 原生视图** | 🔒 确定 |
| 有没有服务端渲染 | **有，但仅限少数 WebView 类组件**（图表/地图）：加载 OpenAI CDN 上的**远端 HTML 页面**渲染，再嵌进原生界面 | 🔒 确定 |
| 模型回复的格式 | **JSON 文档（DIL）**：节点是对象，必须有字符串 `type`，`children` 必须是数组；v2 根节点还必须有字符串 `code` | 🔒 确定 |
| 组件如何被引用 | 用**已注册的组件名** + 服务端 `resolutionId`；名字未注册直接报 `Unknown DIL widget type` | 🔒 确定 |
| 数据从哪来 | **不由模型直接塞**：模型声明工具调用 → 服务端执行 → 回填到 `opGenui.componentResults[resolutionId].state` | 🔍 确证 |
| 是否支持流式 | 支持：文档以 **JSON Patch 追加**方式边生成边渲染，未写完也能出首帧 | 🔒 确定 |
| 不可信代码在哪跑 | **本地「原生 QuickJS + 专用线程」沙箱**（**不是 WASM**）——引擎为 `libopenai_xplat_export.so` 内的 AArch64 原生 QuickJS，全局对象冻结、回调只允许纯 JSON、带超时 | 🔒 确定（设备级证据，见 §7.1） |

一句话：**服务端负责「生成描述 + 解析数据」，客户端负责「编译 + 渲染」；只有 WebView 类组件才把渲染交给远端 HTML。**

---

## 二、方法与证据等级

| 等级 | 含义 |
|---|---|
| 🔒 **确定** | 直接读到源码符号、字符串常量或字节，可复现 |
| 🔍 **确证** | 多个独立证据相互印证 |
| 💡 **推断** | 由符号命名与结构合理推测，**未读取实现体** |
| ❓ **未知** | 证据不足 |

**证据形态（重要）**：本次材料来自 APK `assets/` 下的 Valdi 模块（`dil_core` / `dil_compiler` / `dil_renderer` / `dil_sandbox` / `dil_widgets` / `dil_generated_components` 等）。

- `dil_generated_components.js`（5.25 MB）内含**未混淆的组件源码**（保留 `//#region <组件名>.dil.tsx` 注释）→ 该部分结论为 🔒。
- `dil_compiler.js` / `dil_renderer.js` / `dil_sandbox.js` / `dil_core.js` 中，**实现体不可读**，只能读到符号表与错误消息 → 该部分以 🔒（符号/错误串本身）+ 💡（行为推断）为主。
- ❗ 一处方法论纠错：**不要把 HBC 字符串表当普通文本用正则找带引号的键名**（如 `"code"`）。本次试过 `\x04code` 字节模式，连已确认存在的 `type` 都搜不到，说明长度前缀假设不成立 —— 该技法已废弃，未据此下任何结论。

---

## 三、投递链路：模型 → 服务端 → 客户端

### 3.1 组件的「数据」通道：`opGenui`（🔒 确定，来自可读源码）

组件通过 App Data 读取，字段全集由源码统计得出：

```js
const opGenui = DIL.useAppData((appData) => appData.opGenui);
```

| 字段 | 出现次数 | 说明 |
|---|---|---|
| `componentResults` | 668 | **组件解析结果表**（新协议） |
| `componentData` | 436 | 旧协议：直接存裸状态 |
| `surface` | 32 | 宿主表面，值如 `"live-activity"` |
| `supportedClientActions` | 16 | **客户端被授权的动作集**（见姊妹篇） |
| `showFailedComponents` | 2 | 是否显示失败组件 |
| `supportsWidgetVoiceSessions` | 1 | 语音会话支持 |
| `isShared` | 1 | 是否处于共享会话 |
| `nativeServerActions` | 1 | 原生服务端动作 |
| `modelDataBindings` | 1 | 模型数据绑定 |
| `hideInaccessibleFiles` | 1 | |
| `linkLabels` / `hostLinkUrls` | 各 1 | 链接标签与宿主链接 |

**解析结果信封**（🔒 确定，源码明文）：

```js
function isComponentResultEnvelope(value) {
    if (typeof value !== "object" || value == null || Array.isArray(value)) return false;
    const result = value;
    if (result.status === "pending") return true;
    if (result.status === "resolved") return typeof result.state === "object" && result.state != null && !Array.isArray(result.state);
    return result.status === "failed"
        && (result.code === "component_unavailable" || result.code === "tool_failed" || result.code === "invalid_tool_result")
        && typeof result.retryable === "boolean";
}
```

即服务端要给出：

```jsonc
{
  "status": "pending" | "resolved" | "failed",
  "state":  { /* 仅 resolved：组件渲染要用的数据，必须是非数组对象 */ },
  "code":   "component_unavailable" | "tool_failed" | "invalid_tool_result",  // 仅 failed
  "retryable": true | false                                                    // 仅 failed
}
```

**键名规则**（🔒 确定）：

```js
const key = componentDataKey(componentName, props);
function componentDataKey(componentName, props) {
    return JSON.stringify([componentName, normalizeComponentProps(props)]);
}
```

→ 查找顺序：先按 `props.__resolutionId` 查 `componentResults[resolutionId]`，查不到再按**缓存键** `JSON.stringify([组件名, 规范化 props])` 查；两个通道都没有则回落旧协议 `componentData[key]`。

**`normalizeComponentProps` 的排除表**（🔒 确定，写协议时要对齐）：

```js
if (key === "__resolutionId" || key === "__state" || key === "children" || key === "fallback") continue;
```

### 3.2 「数据由工具调用产出」（🔍 确证）

失败码里出现 **`tool_failed`** 与 **`invalid_tool_result`** —— 这直接说明组件数据是**模型声明工具调用、服务端执行后回填**，而不是组件自己去取数据。完整闭环见姊妹篇 §13.4。

### 3.3 消息级关联字段（🔒 符号确证）

客户端按 message / conversation 维度关联一次 DIL 渲染，遥测标签中有：

```
messageMetadata.dilJson          DIL 文档对象
dilJsonObject.dilJson            DIL 文档对象
isModelWrittenDIL                是否模型书写
is_legacy_dil_version            是否旧版协议
dilVersion                       协议版本（当前 2.0）
dil_url_present / dilUrlPresent  是否带远端 URL
_dil_message_id / _dil_conversation_id / _dil_content_reference_index
source_generation / renderingSourceGeneration / latestSourceGeneration
```

- ❓ **未知**：该文档具体挂在 HTTP/SSE 的哪个字段（未读 dex 网络层与协议实现）。
- 🔍 **确证**：遥测事件名中出现 **`chatgpt.model_written_dil.v2.render`** 与 **`dil.widget.legacy_v1_render_attempt`** —— 即"模型书写的 DIL v2"与"遗留 v1"是两条并行路径，且**同属客户端渲染**。

---

## 四、DIL 文档格式规范

### 4.1 节点结构（🔒 确定，来自校验器错误串）

`src/DILJsonValidation.js` 的校验规则即格式定义：

| 校验规则（错误串原文） | 含义 |
|---|---|
| `Invalid DIL JSON: failed to parse JSON string.` | 文档是 **JSON 文本** |
| `Invalid DIL node: expected an object.` | 节点必须是**对象** |
| `Invalid DIL node: expected string "type".` | 节点必须有**字符串** `type` |
| `Invalid DIL children: expected an array.` | `children` 必须是**数组** |
| `DIL requiredComponents must be an array of strings.` | 顶层 `requiredComponents` 必须是**字符串数组** |

配套：`DILParser.parseDILJson` / `validateDILJsonValue`、`DILJsonValidation.getDILNodeValidationError` / `getDILChildrenValidationError`、`DILNodePath`（节点路径，形如 `root.children[0].children[2]`）。

### 4.2 版本 V1 / V2 与 `code` 属性（🔒 确定）

| 证据 | 内容 |
|---|---|
| `DILCompiler.validateV1Json` / `compileDILV1` / `incrementalCompileDILV1` | 有 **V1** 通道 |
| `compileDILV2` / `compileDILV2FromJsonValue` / `isDILV2` | 有 **V2** 通道 |
| **`Expected DIL v2 JSON root to include a string code property.`** | **V2 根节点必须含字符串 `code`** |

💡 **推断（强）**：`code` 里装的是**组件源码字符串**。理由是：客户端具备 JS 源码编译/求值能力（`DILRenderer.compileJS` / `evaluateJS`、`DILSourceEvaluator`、`DILDynamicSourceEvaluator.evaluateDILSource`），且生成组件本身就以**源码字符串**形式存在（见第八节 `componentSource`）。
💡 **备选解释**：`code` 是组件标识/编码名。两种解释都未读到实现体，故标推断。

### 4.3 控制流用「内建 widget」（🔒 确定）

`src/DILIntrinsicWidgets.js`：`INTRINSICS_WIDGETS_MAP`

| 内建 | 作用 |
|---|---|
| `Each` | 列表迭代 |
| `Scope` | 作用域/变量绑定 |
| `Show` | 条件为真时渲染 |
| `Show.else` | 条件分支 |

→ 即 DIL 里**没有 if/for 语法**，条件与循环通过这几个内建节点表达。

### 4.4 绑定与表达式：两套（🔒 确定）

| 方案 | 证据 |
|---|---|
| **CEL 表达式** | `dil_cel`、`cel.js`、`celExpressionResolver`、`CELExpressionResolver`、`DILWidgetState`/`DILScopedState` |
| **JS 源码求值** | `DILRenderer.compileJS` / `evaluateJS`、`DILDynamicSourceEvaluator.evaluateDILSource`、`DILSourceEvaluator`、`__dil.jsx`、`jsxRuntimeFacade` |

`DILSourceEvaluator` 暴露给组件代码的运行时 API（🔒 符号）：

```
useAppData / useConstants / useCallback / useEffect / useId / useMemo / useRef / useNow
useState / useTheme / useBreakpoint / useIsMobile / useDictation / useSpeechSynthesizer
useDILMessenger / useHtmlViewMessenger
key / widgetId
```

→ 组件代码在**沙箱**里拿到一套受限的 React 风格运行时（第八节）。

### 4.5 懒加载已注册组件（🔒 确定）

`src/DILSandboxSource.js`：

```
resolveDILRequiredComponents
DIL_LAZY_COMPONENT_NAMES.filter(...).includes(...)
__dil.jsx(...)
"DIL requiredComponents must be an array of strings."
```

→ 文档可以列出 `requiredComponents`（字符串数组），客户端据此按需加载**随包发布**的组件源码（`DILComponentSources`），再由沙箱求值。

### 4.6 流式增量（🔒 确定）

| 证据 | 内容 |
|---|---|
| `src/DILStreamingJSONPatch.js`（dil_core） | `DILStreamingJSONPatchOperationKind`、`makeDILStreamingJSONPatch` |
| 操作种类 | **`Append`** / `Replace` / `Remove` / `Add`，并带 `~0` / `~1`（JSON Pointer 转义） |
| `json_patch/src/ApplyJSONPatch`（dil_compiler 依赖） | 客户端本地应用补丁 |
| `DILRootComponent.applyJsonPatch` | 根组件应用补丁 |
| `DILIncrementalCompileJSONPatchVisitor` | 增量编译访问器 |
| `DILStreamingRenderState` / `updateStreamingState` / `isSourceStreaming` | 流式渲染状态 |

→ **模型边生成、客户端边补丁边渲染**；`Append` 是主操作（文档是"长出来"的）。遥测印证：`didEmitFirstVisibleToken` / `didEmitLastVisibleToken` / `time_to_first_visible_token_ms`。

### 4.7 格式校验清单（反向定义，🔒 全部为原文错误串）

任何实现方都可以用这张表自查：

```
Invalid DIL JSON: failed to parse JSON string.
Invalid DIL node: expected an object.
Invalid DIL node: expected string "type".
Invalid DIL children: expected an array.
Expected DIL v2 JSON root to include a string code property.
Expected DIL entry point initialState to be a JSON object.
Unknown DIL widget type "%s".
Unknown DIL element type "%s".
DIL requiredComponents must be an array of strings.
Failed to apply DIL style property "%s".
Missing copy semantics for DIL renderer "%s".
DILCompiler.incrementalCompile requires a DIL compile result from a previous compile call.
Expected a DIL incremental compile result...
DIL.h can only be called while rendering a DIL component.
Unbalanced DIL render node stack.
```

---

## 五、渲染侧归属：本地渲染

### 5.1 客户端持有完整管线（🔒 确定）

客户端的导入段把整条管线 API 完整列出（`dil_widgets.js @1,408,352`，位于 `src/DILSandboxTraceEnvelope.js` 模块的导入表）：

```
DILCompiler.parse → validateV1Json → compileTree → compile → incrementalCompile
DILRenderer.compileJS → evaluateJS → loadComponent → render
DILRootComponent.onEvent / copyContent / applyJsonPatch
DILEntryPointComponent.onCreate / onViewModelUpdate / onDestroy / createDILContext / reportError
WidgetsRegistry.createDIL_WIDGETS_MAP
ElementsRegistry.createDIL_ELEMENTS_MAP
DILSandbox.WorkerSendAcknowledgement / HostReceiveAcknowledgement
```

模块分层（各 bundle 实测模块数）：

| bundle | 模块数 | 职责 |
|---|---|---|
| `dil_core` | 42 | 根组件、样式/布局/颜色、流式 JSON Patch、XML 构建 |
| `dil_compiler` | 36 | 解析、JSON 校验、属性 schema、编译、增量补丁访问器 |
| `dil_renderer` | 26 | 渲染器、API facade、JSX 运行时、状态持久化、HTML view 信使 |
| `dil_sandbox` | 37 | QuickJS 沙箱、worker、协议、超时、追踪 |
| `dil_widgets` | 228 | 元素渲染器 + widget + 动画 + 复制语义 + 动作授权 |
| `dil_generated_components` | 288 | 预置组件（源码字符串） |

→ **解析器、编译器、渲染器、沙箱全在客户端**。渲染终点是 `WidgetNode → Valdi → 原生视图`。

### 5.2 渲染产物本地自检（🔒 确定）

`DILRenderOutputMonitor` / `DILRenderOutputInspection` / `DILRenderHealthMetrics`：

```
inspectDILRenderOutput / layoutChanged / completion / DILRenderOutputStatus.Content
"DIL response has suspicious settled render output"
```

→ 客户端会**检查自己渲染出来的结果**是否"看起来正常"（内容是否存在、尺寸是否为零等），这只有在本地渲染才需要。

### 5.3 平台维度（🔒 确定）

遥测维度含 `platform: valdi / web / ios / android`，并有 `isWeb` / `isDesktop` 判断 —— 同一份 DIL 在不同平台渲染，进一步排除"服务端渲染 HTML 再回传"的主路径。

---

## 六、混合例外：WebView 与远端 HTML

**这是唯一一处"远端渲染"**（🔒 确定）。`src/webview/DILWidgetWebUrl.js`（`dil_widgets.js @984,320`）暴露：

```
★ https://cdn.platform.openai.com/deployments/widgets/index.html
   —— 从 OpenAI CDN 加载的 widgets 宿主页面
dilPayloadId
```

postMessage 协议（消息类型，原文）：

| 消息类型 | 作用 |
|---|---|
| `dil-widget` | 下发 widget 内容 |
| `dil-configure-widget` | 配置 |
| `dil-set-padding` | 设置内边距 |
| `dil-set-state` | 同步状态 |
| `dil-set-theme` | 同步主题（明/暗） |
| `dil-widget-action` | 页面 → 宿主：动作 |
| `dil-widget-action-response` | 宿主 → 页面：动作应答 |

跨端实现细节（源码明文）：

```js
(function() {
  window.dispatchEvent(new MessageEvent('message', { data: <base64> }));
  window.dispatchEvent(new MessageEvent('message', { data: <base64> }));
})();
```

- 载荷用 `TextEncoder` + **Base64（urlSafe）** 编码后经 `postLoadJavaScript` 注入
- 事件为 `MessageEvent`，并带 `sourceId`、`window.origin || window.location.origin || '*'`
- 该路径下的组件：`PieChartWebWidgetRenderer`、`ChartWebWidgetRenderer`、`MapSurface`/`NativeMapWebViewSurface`（Mapbox GL JS）

**准确表述**：这类组件的 **UI 代码来自远端 CDN**、在 WebView 内执行；**数据仍由本地通过 postMessage 注入**。因此它是"远端代码 + 本地执行"，与"服务端渲染好一张位图/HTML 回传"不是一回事。

### 6.1 远端资源策略（🔒 确定）

| 机制 | 证据 |
|---|---|
| 可全局禁止远端资产 | **`DENY_REMOTE_DIL_ASSETS`**（dil_core 渲染上下文选项） |
| 资产 URL 授权 | `DILAssetURLAuthorization`、`assetURLAuthorization`、`isAllowedAssetURL` |
| 网络策略白名单 | `DILTrustedNetworkPolicy.allowedUrlRegexes`（详见姊妹篇 §7.1） |
| 图片可要求审批 | `imageApprovalRequired`、`sensitiveImageBlurEnabled` |

---

## 七、不可信代码的本地沙箱

**关键点：模型/服务端下发的代码是在本地跑的，但被关进沙箱**（🔒 确定）。

| 机制 | 证据 |
|---|---|
| 引擎 | **原生 QuickJS（AArch64 ELF），不是 WebAssembly**：命名空间 `ValdiQuickJS::QuickJSJavaScriptContextFactory` / `QuickJSJavaScriptContext`，含 **170 个 `JS_*` C API 符号**；JS 侧以 `ValdiWASMModule_quickjs` 引用它。详见 §7.1 |
| 隔离 | 跑在 **Worker** 里：`DILSandboxWorker` / `DILSandboxWorkerCore` / `DILSandboxWorkerTransport` / `startDILSandboxWorker` |
| 全局冻结 | `recursivelyFreezeAllowedGlobals`、`freezeObjectGraph`、`"Failed to freeze an object in the object graph."` |
| API 硬化 | `hardenApiFacade`（`DILApiFacade`）、`createDILGenUIFacade`（全量）vs **`createDILModelGenUIFacade`（模型专用精简版）** |
| 回调边界 | `"DIL callback results must be plain JSON data or undefined."`、`"DIL callback results cannot contain cyclic data."` |
| 超时 | `DILSandboxTimeout`、`DIL_SANDBOX_DEFAULT_MESSAGE_TIMEOUT_MS`、`"Timed out waiting %dms for DIL input to complete."`、`dil-sandbox-timeout` |
| 崩溃兜底 | `DILSandboxCrashView`、`hasSandboxPermanentlyFailed` |
| 内建库也自实现 | `DILSandboxIntl`（**只支持 en-US 的精简 `Intl.NumberFormat`**）、`DILSandboxRegExp`（`"Sandbox RegExp requires the native source getter."`） |
| 追踪 | `DILSandboxTrace`、`dil_sandbox_trace`、`DILTraceCollector` |

### 7.1 ✅ 沙箱引擎定性：原生 QuickJS，**不是** WebAssembly（设备级证据）

⚠️ **重要更正**：早期版本的本报告曾写作"QuickJS 编译成 WASM"。**该判断是错的** —— 那是把 Valdi 的模块抽象名 `ValdiWASMModule_quickjs` 误读成了运行时技术。从设备上取回真实分片后已定论（全 🔒）。

#### 7.1.1 设备上真实安装的全部分片

设备：Redmi 6 Pro / Android 9 / API 28 / arm64-v8a；`pm path com.openai.chatgpt` 返回：

```
base.apk                     50,194,540 B
split_config.arm64_v8a.apk  133,108,787 B   ← 11 个原生库全在这里
split_config.xxhdpi.apk          78,871 B
split_config.zh.apk           1,462,482 B
```

`split_config.arm64_v8a.apk` 内的 11 个 `.so`（均为 **STORED 未压缩**，故可在设备上原地 grep）：

| 库 | 未压缩大小 |
|---|---|
| `liblkjingle_peerconnection_so.so` | 108,450,520 B |
| **`libopenai_xplat_export.so`** | **17,298,088 B** ← **QuickJS 在这里** |
| `liborbit_characters_jni.so` | 3,251,864 B |
| `libcapture.so` | 2,656,136 B |
| `libsentry.so` / `libsentry-android.so` | 853,584 / 20,024 B |
| `librenderscript-toolkit.so` | 394,112 B |
| `libimage_processing_util_jni.so` / `libdatastore_shared_counter.so` / `libandroidx.graphics.path.so` / `libsurface_util_jni.so` | ≤ 32,528 B |

#### 7.1.2 QuickJS 是原生 AArch64 代码

从 `libopenai_xplat_export.so` 切出、裁掉 ZIP 本地头后得到 17,313,792 B 的对象：

| 检查项 | 结果 |
|---|---|
| ELF 头 | `ELF64 / little-endian / ET_DYN / e_machine=183 (AArch64)` → 真实原生共享库 |
| **`JS_*` C API 符号** | **170 个**（`JS_CallInternal`、`JS_DefinePropertyValueValue`、`JS_AtomGetStrRT`、`JS_CallConstructor` …）→ QuickJS 官方 C API |
| C++ 符号（`ValdiQuickJS` 命名空间） | `QuickJSJavaScriptContextFactory`、`QuickJSJavaScriptContext`、`NativeClassFunctionData`；`createJsContext(Valdi::JavaScriptTaskScheduler*, Valdi::ILogger&)`、`requiresDedicatedThread()`、`handleInterrupt(JSRuntime*, void*)`、`handleRejectedPromise(JSContext*, JSValue, JSValue, int, void*)`、`freeArrayBuffer`、`freeByteBuffer`、`dumpHeap` |
| **WASM 魔数 `00 61 73 6d`** | **0 处** |
| `emscripten` / `wasi` / `wasm` / `WebAssembly` 字符串 | **0 / 0 / 0 / 0** |
| 依赖 | `libc` `libm` `libdl` `liblog` `libz` `libandroid` `libEGL` `libGLESv3` `libjnigraphics` `liborbit_characters_jni.so` |

#### 7.1.3 决定性字符串：该 build 只有 QuickJS，没有 JSCore

```
"Requested JSCore VM, which is not available on this build type."
```

→ Valdi 的 JS 引擎是可插拔的（JSCore / QuickJS / …），**Android 这个 build 只带 QuickJS**。

#### 7.1.4 `ValdiWASMModule` 是模块系统术语，不代表运行时用了 WebAssembly

JS 侧的 `ValdiWASMModule_quickjs` + `getExportedModule` + `createExportedFunction` + `invokeFunction` 属 Valdi 的**模块 ABI 抽象**（把 C/C++ 实现以导出函数形式暴露给 JS）。在 Android/arm64 上，该模块由**原生 QuickJS** 承担；**全 app 不存在 WASM 运行时或 `.wasm` 模块**。

#### 7.1.5 沙箱隔离由原生层实现（与 JS 侧观察一一对应）

| JS 侧现象 | 原生侧对应 |
|---|---|
| `DILSandboxWorker` / `startDILSandboxWorker` | `QuickJSJavaScriptContextFactory::requiresDedicatedThread()` |
| `DILSandboxTimeout` / `dil-sandbox-timeout` | `handleInterrupt(JSRuntime*, void*)`（QuickJS 中断回调 → 超时打断） |
| 回调只允许纯 JSON / 无循环引用 | `handleRejectedPromise`、`freeArrayBuffer`、`freeByteBuffer` |
| 渲染健康度监控 | `dumpHeap` |

#### 7.1.6 对「有没有 WASM 沙箱」的最终答复

> **没有 WASM 沙箱。有的是「原生 QuickJS + 专用线程」沙箱。**
> - "WASM" 只出现在 Valdi 的**模块抽象命名**中，不是运行时技术
> - 引擎实体：`split_config.arm64_v8a.apk` → `libopenai_xplat_export.so`（AArch64 ELF，170 个 `JS_*` 符号）
> - dex 中唯一的 `WebAssembly` 字符串夹在互不相关的字符串表条目之间（`WebBillingGetProducts` / `WebBillingPhase` / Network Callback 报错文案），是普通 interned 字符串，**不构成 WASM 运行时证据**

#### 7.1.7 取证路径（可复现）

| 步骤 | 命令 / 工具 |
|---|---|
| 列分片 | `adb shell pm path com.openai.chatgpt` |
| 列库名（不必拉 127 MB） | `adb exec-out "tail -c 300000 <split.apk> \| base64"` → 本地解析 ZIP 中央目录 |
| 归属定位（哪个库含某符号） | 设备端 `tail -c +OFF \| head -c SIZE \| grep -a -o -i <pat> \| wc -l` |
| 取回样本 | `adb shell dd if=<split.apk> of=/sdcard/x.so bs=4096 skip=27149 count=4230` → `adb pull` |
| 判定 | ELF 头 + `JS_*` 符号 + WASM 魔数扫描；REA `open_binary` 独立确认 `format=elf / kind=executable / architecture=arm64`（sha256 `359894375ee3ba89366a09fdc2a13895aa889491fe87a25b9cfd131903066d53`） |

> 工具可用性备注：REA 的 `inspect_binary_layout`（pwntools profile）**只支持 Linux x64**，无法分析该 AArch64 ELF；Ghidra provider 未配置（`GHIDRA_INSTALL_DIR` 未设，`D:\ReverseTools\ghidra` 已存在可修复）。要对此 `.so` 做伪代码级分析，需先补这两项之一。
>
> 早前两处误读已更正：① "QuickJS 编译成 WASM" → 实为原生实现；② `assets/orbit-runtime/`（`heroes/*.orbitmesh`、`fitted/balanced/*.orbitfaces`、`meshes/balanced/authored-crown|beret|eyewear`）是 **3D 头像资源**，与 JS 引擎无关，其中 `orbittable` 里的 `qjs` 是二进制巧合。

**沙箱内可见的 GenUI API**（🔒 符号，来自 `DILApiFacade`），即模型书写的 UI 能调用的全部能力：

```
GenUI.copy(text)              "GenUI.copy requires one text string."
GenUI.issueNewTurn(query)     "GenUI.issueNewTurn requires one query string."
GenUI.openUrl(url)            "GenUI.openUrl requires one URL string."
GenUI.openEntityDetail(...)   "requires one reference or named entity..."
GenUI.openImageLightbox(...)
GenUI.runPluginTool(name, args)  "requires one tool name and JSON arguments object."
GenUI.logEvent(...)
GenUI.dispatchAction(action)  "requires an action object." / "requires payload to be an object."
                              "requires %s to be a string."
                              "exceeds the maximum fallback depth."
"Registered component action is missing its bound target."
DIL_PLUGIN_TOOL_UNCONFIRMED
```

> 注意 `createDILGenUIFacade` 与 **`createDILModelGenUIFacade`** 是两个不同入口 —— 与姊妹篇的 `isModelWrittenDIL` 信任分级一致：**模型书写的 UI 拿到的 API 是精简版**。

---

## 八、组件以「源码字符串」形式携带

生成组件的模块导出的是**源码文本**（🔒 确定，明文可读；`componentSource` 在 `dil_generated_components.js` 中出现 **284 次**）：

```js
// src/restricted/components/WordCardWidget.generated.js
module.exports.__esModule.componentSource:
"(createComponent) => {
   const hostGenUI = GenUI;
   const unscopedComponent = createComponent(DIL, hostGenUI);
   function ResolvedComponentWordCardWidget(props) {
     const resolutionId = props.__resolutionId;
     const Component = DIL.useMemo(() => {
       if (typeof resolutionId !== \"string\") return unscopedComponent;
       ...
```

沙箱侧的求值方式（🔒 符号）：

```
DILSandboxComponentGlobals：注入 DIL / __dil / GenUI
包装为："use strict"; return (<componentSource>);
"DIL sandbox component '%s' did not evaluate to a function."
"DIL sandbox component global '%s' already exists."
isDILRestrictedComponent / __state / __resolutionId
OpGenuiResolvedComponentBoundary
```

**这解释了"模型写 UI"的形态**：模型产出的是**代码/文档**，客户端先编译再在沙箱里求出组件函数，最后渲染为原生视图 —— 全程不依赖服务端渲染。

---

## 九、复现步骤与证据清单

**输入**：`d:\MyApplication\com.openai.chatgpt_1791789377928.apk`
**工作目录**：`%TEMP%\cgpt\`

| 脚本 | 作用 |
|---|---|
| `proto2.py` | 各 bundle 模块清单 + 投递格式关键词命中统计 |
| `proto3.py` | 编译器/渲染器 API、关键模块字符串表（DILJson / DILJsonValidation / DILParser / DILStreamingJSONPatch / DILSandboxProtocol …） |
| `proto4.py` | HTML/WebView 子路径、`code` 属性、投递通道、版本选择 |
| `proto5.py` | 源码求值入口、`componentSource`、模型可见 GenUI API |
| `proto7.py` | 从可读源码统计 `opGenui` 字段全集 |
| `format_check.py` / `probe_fmt.py` | 判定 bundle 容器格式与内容可读性 |
| `apk_wasm.py` / `apk_wasm2.py` | APK 条目清点：`*.wasm` / `lib/*.so` / 全条目字节扫 WASM 魔数 |
| `wasm_deep.py` / `wasm_all.py` | 用 zstandard 解压全部 84 个 `.valdimodule` 后扫 WASM 魔数与 QuickJS 关键字 |
| `apk_orbit.py` | 追查 `assets/orbit-runtime/` 与 native 线索（判定与其无关） |
| `dex_solibs.py` | 定向提取 dex 中引用的 `.so` 名与 `valdi`/`quickjs`/`wasm` 关键词 |
| **设备侧取证（adb）** | `pm path` 列分片 → `adb exec-out "tail -c N \| base64"` 取 ZIP 尾 → 设备端 `tail\|head\|grep` 原地定位 → `dd`+`pull` 取回目标 `.so` |
| `parse_cd.py` | 解析 ZIP 中央目录（不需拉 127 MB 即可列出 11 个 `.so` 名与偏移） |
| `elf_analyze.py` / `elf2.py` / `trim.py` | ELF 头与节表、WASM 魔数扫描、`quickjs`/`ValdiQuickJS`/`JS_*` 符号上下文、裁掉 ZIP 头得干净 ELF |
| **REA MCP**（`open_binary` → `inspect_artifact` → `binary_session` → `inspect_android_package` → `open_binary`(ELF)） | 权威包清单与清单解码（JADX 0.7.1）；ELF 目标独立确认 `format=elf / architecture=arm64` |
| **本地 aapt2**（`dump badging` / `dump resources` / `dump xmltree`） | 交叉验证 `native-code` 缺失与 `splits0.xml` 分片清单 |

**产物**：`out_proto2.txt`(43 KB)、`out_proto3.txt`(50 KB)、`out_proto4.txt`(30 KB)、`out_proto5.txt`(15 KB)、`out_proto6.txt`(4 KB)、`out_proto7.txt`(6 KB)、`out_probe_fmt.txt`(5 KB)、`out_apk_wasm.txt`、`out_apk_wasm2.txt`、`out_wasm_deep.txt`、`out_wasm_all.txt`(**84 模块解压扫描**)、`out_apk_orbit.txt`(15 KB)、`out_dex_solibs.txt`、`out_split_entries.txt`(11 个 `.so` 清单)、`out_elf.txt` / `out_elf2.txt`(ELF 与符号判定)、`elf/libopenai_xplat_clean.so`(17.3 MB 样本)

**REA 会话证据 ID（可复查）**

```
目标 SHA-256          206851fabaf794e7d5b52bc1129caf25e4dc3e1e55b8cd6caaa3ac8df952e780
inspect_artifact      ev_b157fdf59f6cf0246e244fe02af48e0033f265739a8ddaaeeb2a8f409f1fb54b（完整清单超出 stdio 预算）
inspect_android_package  ev_90a8fce9ee9701a947e76455ebd2895c24c3c3dc98284390536afde2808bdcad
search_android_classes(wasm)      ev_8463d928896506c830957fcadc372c4252fac271805c7c2c86dfabfec0c1c1bb
search_android_classes(QuickJS)   ev_f353975ec185af1a9066ca5f1b14cc2cd3c2e563afdc140b20bf22b54021d5a3
```

> 工具可用性备注（本机实测）：
> - **`inspect_binary_layout` 只支持 Linux x64**（返回 `capability_unavailable: The initial real-verified pwntools artifact profile supports Linux x64 only.`）→ **无法**分析该 AArch64 ELF。
> - **Ghidra provider 未配置**（`GHIDRA_INSTALL_DIR` 未设；`D:\ReverseTools\ghidra` 已存在，可修复）→ `binary_overview` / `search_strings` 等原生能力不可用。
> - native provider 对 archive 目标不适用；`analyze_javascript_application` 可用。
> - `inspect_artifact` 的父证据**不支持** `inspect_analysis_view`（返回 `unsupported_target`），需改用 `get_evidence_bundle`。
> - dex 已被 R8 全量混淆（74,961 类，`defpackage.*`）→ 按类名搜索无意义。

**关键偏移**：

```
dil_core.js     @128,413   dilJsonObject / parsedInitialState / DENY_REMOTE_DIL_ASSETS
dil_compiler.js @35,396    src/DILJsonValidation.js   ← 节点校验规则
dil_compiler.js @71,085    src/DILCompiler.js 主符号表（V1/V2、code 属性、JSON Patch）
dil_renderer.js @28,841    src/DILSourceEvaluator.js  ← 沙箱内运行时 API
dil_renderer.js @101,786   src/DILDynamicSourceEvaluator.js（compileJS / "use strict"）
dil_renderer.js @112,435   src/DILApiFacade.js        ← 模型可见 GenUI API
dil_sandbox.js  @47,116    src/DILSandboxQuickJSWorker.js（调用 ValdiWASMModule_quickjs）
dil_widgets.js  @952,687   src/webview/WebWidget.js
dil_widgets.js  @984,308   src/webview/DILWidgetWebUrl.js  ← CDN URL + postMessage 协议
dil_widgets.js  @1,408,352 src/DILSandboxTraceEnvelope.js 导入表 ← 完整管线 API
dil_widgets.js  @1,422,805 dil.widget.* 遥测标签
dil_widgets.js  @1,502,297 src/DILEntryPointComponent.js
dil_generated_components.js @190 / @16,959  componentSource（源码字符串）
```

---

## 十、边界与未决问题

**边界**

1. **纯静态**：未运行 APK、未抓包，故"实际线上字段名/HTTP 路径"未验证。
2. **多数 bundle 只读得到符号表**：编译器/渲染器/沙箱的**控制流不可读**，因此"门控顺序""`code` 的具体内容""补丁应用时机"属推断。
3. **未做运行时验证**：没有 hook 过 `DILRenderer.render` 或 `ApplyJSONPatch`。
4. **未读 dex**：工具调用在模型侧的**函数签名/schema 定义**在 Kotlin 层，本次未展开。
5. **✅ 手上的 APK 是 base 分片 —— 已用设备上的完整分片补齐（该限制已解除）**：清单声明 `android:requiredSplitTypes="base__abi,base__density"`；已从设备取回 `split_config.arm64_v8a.apk`（133 MB / 11 个 `.so`）并完成引擎定性（见 §7.1）。**native 侧结论现为 🔒 确定。**

**未决问题**

| # | 问题 | 下一步 |
|---|---|---|
| 1 | DIL 文档挂在 HTTP/SSE 的哪个字段？（`dilJson` 的确切来源） | 抓包 或 读 dex 网络层 |
| 2 | V2 根 `code` 的精确内容是源码还是标识？ | 运行时打断点看 `compileJS` 入参 |
| 3 | 模型侧的**工具调用 schema**（模型如何被要求输出 DIL） | 读 dex 中的工具定义 |
| 4 | v1 → v2 的迁移条件（`isVersionAtLeast` 的判定点） | 读 dex + 服务端实验 |
| 5 | WebView 页面 `cdn.platform.openai.com/deployments/widgets/index.html` 的内容与版本策略 | 网络抓取 + 与本地协议字段比对 |
| ~~6~~ | ~~QuickJS 引擎实体在哪？~~ **已结案**：实体为 `split_config.arm64_v8a.apk` → `libopenai_xplat_export.so`（AArch64 原生 QuickJS，非 WASM），见 §7.1 | — |
| 7 | 原生侧尚未做**伪代码级**分析：`ValdiQuickJS` 的沙箱加固细节（全局白名单、回调序列化、中断策略） | 配置 `GHIDRA_INSTALL_DIR` 后用 Ghidra/REA 分析 `libopenai_xplat_export.so` |
| 8 | `liblkjingle_peerconnection_so.so` 为何达 108 MB（远超常规 WebRTC） | 拉取后做段/符号/字符串画像 |

---

*本文由静态逆向分析产出，记录于 2026-10-12。所有 🔒 级结论均可用第九节脚本复现。姊妹篇：[`CHATGPT_DIL_CLIENT_ACTIONS.md`](./CHATGPT_DIL_CLIENT_ACTIONS.md)*
