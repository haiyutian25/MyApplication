# ChatGPT DIL 客户端动作授权分析

对官方 ChatGPT Android 客户端（`com.openai.chatgpt`）中 **DIL**（服务端下发的界面描述引擎）的**客户端动作授权体系**的分析：服务端能要求客户端执行哪些动作、这些动作受什么门控、以及客户端如何拒绝越权请求。

本文是 [`CHATGPT_APK_ANALYSIS.md`](./CHATGPT_APK_ANALYSIS.md) 的专门续篇 —— 该文第十二、十三节给出 DIL 的整体架构与运行时协议，本文只聚焦**动作授权**这一条线。

「服务端要下发什么格式」与「在哪一侧渲染」另见姊妹篇 [`CHATGPT_DIL_FORMAT_AND_RENDERING.md`](./CHATGPT_DIL_FORMAT_AND_RENDERING.md)。

- 分析日期：2026-10-12
- 目标文件：`com.openai.chatgpt_1791789377928.apk`
- 目标版本：`versionName 1.2026.279` / `versionCode 2627909`
- 分析方法：静态分析（**未运行**该 APK，未连接其服务）
- 分析对象：`dil_widgets.js`（1.62 MB，DIL 基础库）、`dil_generated_components.js`（5.25 MB，生成组件）

## 目录

- [一、核心结论](#一核心结论)
- [二、分析方法与证据等级](#二分析方法与证据等级)
- [三、三层授权模型](#三三层授权模型)
- [四、动作全集](#四动作全集)
- [五、门控矩阵](#五门控矩阵)
- [六、逐动作 payload 校验](#六逐动作-payload-校验)
- [七、URL / 沙箱 / 文件的三重信任](#七url--沙箱--文件的三重信任)
- [八、服务端动作中继](#八服务端动作中继)
- [九、逐组件授权（源码级证据）](#九逐组件授权源码级证据)
- [十、配套的视图状态写回](#十配套的视图状态写回)
- [十一、值得单独关注的几点](#十一值得单独关注的几点)
- [十二、复现步骤与产物](#十二复现步骤与产物)
- [十三、本次分析的边界](#十三本次分析的边界)
- [十四、未完成的方向](#十四未完成的方向)

---

## 一、核心结论

DIL 的客户端**不是**"服务端给什么动作就执行什么"。一次动作要真正被执行，必须同时通过**三层**判定：

```
① 主机能力广告     getHostSupportedClientActions()          ← 本宿主支持哪些动作
        ×
② 能力派生         getEffectiveDILSupportedClientActions()   ← 由 11 个 *_CAPABILITY 常量 + 当前渲染上下文推导
        ×
③ 逐动作校验       DILHostActionsHandler.dispatchClientAction() ← payload 校验 + 前置就绪 + 信任等级 + 手势要求
```

三层全过才执行；任一层不过即丢弃并经 `onDispatchError` / `reportWarning` 上报。

三个关键设计：

| 设计 | 说明 |
|---|---|
| **能力是"派生"的，不是"声明"的** | 可用动作随上下文动态增减（只读？实体数据就绪？共享会话？插件工具就绪？），而非一张静态表 |
| **信任分级** | `isModelWrittenDil`（模型生成的 DIL）被**显著削减**权限：服务端动作对它完全不可用 |
| **UI 侧提前裁剪** | 组件用 `supportedClientActions.includes(...)` 直接决定按钮是否存在，而不只是"点了被拒" |

---

## 二、分析方法与证据等级

| 等级 | 含义 |
|---|---|
| 🔒 **确定** | 直接读到源码符号、字符串常量或字节，可复现 |
| 🔍 **确证** | 多个独立证据相互印证 |
| 💡 **推断** | 由符号命名与结构合理推测，**未读取实现体** |
| ❓ **未知** | 证据不足 |

**关于 bundle 格式（重要，影响证据等级）**：

`dil_widgets.js` / `dil_generated_components.js` 等文件头 4 字节均为 `33 c6 00 01`，随后是一个等于「文件长度 − 8」的 u32 —— 这是 Valdi 自有的模块容器（**不是** Zstd 帧 `28 B5 2F FD`，也**不是** Hermes magic `C6 1F BC 03`）。容器内**混合三种内容**：

| 内容 | 可读性 | 本次用途 |
|---|---|---|
| 符号/字符串表 | 可读（长度前缀编码） | 动作名、能力常量、错误消息、字段名 → 🔒 |
| i18n 文案（`strings/strings-<locale>.json`） | 可读 | 辅助识别能力（如 `show_sensitive_image`） |
| 组件源码 | **可读**（部分区段 UTF-16 编码） | 逐组件授权逻辑 → 🔒 |

> 因此在 `dil_generated_components.js` 中能直接读到**未混淆的组件源码**（含 `//#region <组件名>.dil.tsx` 原始文件名注释），这部分属于 🔒 确定级。而 `dil_widgets.js` 中动作系统的**实现体**读不到，只能读到符号与错误消息，因此该部分结论以 🔍/💡 为主。

---

## 三、三层授权模型

### 3.1 判定函数清单（🔍 确证）

来源：`dil_widgets.js @1,468,142`（模块 `src/DILHostActionsHandler.js`）

| 函数 | 职责 |
|---|---|
| `getHostSupportedClientActions` | 读出宿主广告的动作集 |
| `getEffectiveSupportedClientActions` | 结合上下文裁剪 |
| `getEffectiveDILSupportedClientActions` | 结合能力常量裁剪（见 4.2） |
| `isClientActionSupported` | 单动作判定 |
| `validateClientAction` | payload 校验 |
| `resolveClientAction` | 解析动作目标与 handler |
| `dispatchClientAction` → `dispatchAuthorizedClientAction` → `dispatchClientActionRequest` | 派发链 |
| `dispatchWidgetAction` / `dispatchRegisteredServerAction` | 组件/服务端动作派发 |
| `validateReadOnlyAction` | 只读模式判定 |
| `isSharedConversationAction` / `isSharedPersonalityQuizAction` | 共享会话/共享测验判定 |
| `isReadOnlyEntityReferenceAction` / `isReadOnlySandboxDownloadAction` | 只读下的实体/沙箱判定 |
| `getInteractionContext` | 取 `widgetName` / `dilVersion` / `loggingMetadata` |
| `isVersionAtLeast` | 协议版本能力判定（当前为 `2.0`） |

### 3.2 依赖的动作数据来源

组件通过 App Data 读取被授权动作（🔒 源码级，见第九节）：

```js
const supportedActions = DIL.useAppData((data) => data.opGenui?.supportedClientActions);
const canShare = DIL.useAppData((appData) => appData.opGenui?.supportedClientActions?.includes("share") === true);
```

→ 授权结果由宿主通过 `opGenui.supportedClientActions` 注入到组件上下文，**组件与动作派发链共享同一份授权集**。

---

## 四、动作全集

### 4.1 层 1：基础 host action types（16 项，🔒 确定）

来源：`dil_widgets.js @1,468,142` 的动作类型列表 + 逐动作校验分支

| # | 动作 | 语义 | 关联校验（错误串） |
|---|---|---|---|
| 1 | `start_voice_mode` | 进入语音模式 | `Voice is unavailable for this component.` |
| 2 | `request_media_upload` | 请求用户上传媒体 | `Media upload is unavailable for this component.` |
| 3 | `log_event` | 上报统计事件 | `event_name` / `statsig_event_name` / `event_data` |
| 4 | `copy` | 复制宿主文本 | `isHostCopySupported` |
| 5 | `copy_node` | 复制指定节点 | `payload.target_id must be a non-empty string.` |
| 6 | `issue_new_turn` | 发起新对话轮 | `append_to_current_leaf` |
| 7 | `request_user_input` | 弹表单收集输入 | `payload.questions must be a nonempty array.` |
| 8 | `open_url` | 打开 URL | `untrusted URL in payload.url.` |
| 9 | `open_entity_detail` | 打开实体详情 | `the invoking message is not ready for entity actions.` |
| 10 | `open_image_lightbox` | 打开图片灯箱 | `payload.selected_index is outside payload.images.` |
| 11 | `open_file_citation` | 打开文件引用 | `untrusted file identity in payload.` |
| 12 | `prefill_composer` | 预填输入框 | `payload.query must be a nonblank string.` |
| 13 | `share` | 分享 | `payload.share_options.includes(personality_quiz_share_options)` |
| 14 | `personality_quiz_share` | 性格测验分享 | `Invalid widget sharing intent.` |
| 15 | `personality_quiz_shared_progress` | 分享进度 | — |
| 16 | `personality_quiz_complete_shared` | 完成分享 | — |

> 第 13–16 项在字符串表中位置相邻，💡**推断**同属"分享族"，逐项边界无法从字符串表确证。

### 4.2 层 2：能力常量（11 项，🔒 常量确证 / 💡 配对推断）

来源：`dil_widgets.js @1,619,901`（模块 `src/DILClientActionCapabilities.js`）

**完整性已确认**：跨全部 6 个 bundle 扫描 `*_CAPABILITY` 模式，**该模块只导出这 11 个常量，无其他能力常量**。

| # | 能力常量 | 派生动作类型（💡 推断配对） | 含义 |
|---|---|---|---|
| 1 | `DIL_RESOLVED_WIDGET_VOICE_CAPABILITY` | `start_voice_mode_resolved_widget` | 已解析组件内的语音 |
| 2 | `DIL_WIDGET_VOICE_SESSION_CAPABILITY` | `start_voice_mode_widget_session` | 组件语音会话 |
| 3 | `DIL_RESOLVED_WIDGET_MEDIA_UPLOAD_CAPABILITY` | `request_media_upload_resolved_widget` | 已解析组件内的上传 |
| 4 | `DIL_ENTITY_PINNED_IMAGE_CAPABILITY` | `open_entity_detail_pinned_image` | 打开钉住的图片实体 |
| 5 | `DIL_ENTITY_REFERENCE_CAPABILITY` | `open_entity_detail_reference` | 打开实体引用 |
| 6 | `DIL_READ_ONLY_ENTITY_REFERENCE_CAPABILITY` | `open_entity_detail_reference_read_only` | 只读模式下的实体引用 |
| 7 | `DIL_ENTITY_REFERENCE_READY_CAPABILITY` | `open_entity_detail_reference_ready` | 实体数据就绪后才开放 |
| 8 | `DIL_LITERAL_SANDBOX_PATH_CAPABILITY` | `open_url_sandbox_literal_path` | 只允许**字面**沙箱路径 |
| 9 | `DIL_READ_ONLY_SANDBOX_DOWNLOAD_CAPABILITY` | `open_url_sandbox_read_only` | 只读模式下的沙箱下载 |
| 10 | `DIL_SHARED_CONVERSATION_ACTIONS_CAPABILITY` | `shared_conversation_actions` | 共享会话中的动作 |
| 11 | `DIL_SHARED_PERSONALITY_QUIZ_ACTIONS_CAPABILITY` | `personality_quiz_shared_actions` | 共享测验中的动作 |

> ⚠️ 11 个常量与 11 个派生动作类型在同一字符串表中**顺序 1:1 对应**，配对关系为 💡**推断**（字符串表只记录声明顺序，无指令级证据）。语义高度吻合（如 `LITERAL_SANDBOX_PATH` ↔ `sandbox_literal_path`），置信度高。

**配套判定函数**：`isDILSharedConversationAction` / `isDILSharedPersonalityQuizAction` / `isAllowedModelWrittenDILHostActionType`

`isAllowedModelWrittenDILHostActionType`（模型生成 DIL 的动作白名单）引用了 4.1 的 16 项动作，说明**基础动作集按"谁生成"被分档开放**。

### 4.3 层 3：组件级 affordance（非 host action）

以下标识出现在组件/渲染器中，语义是"控件交互"，**不应与 host action 混为一谈**：

| 标识 | 出现位置 | 性质 |
|---|---|---|
| `copy_node`（本地分支） | `src/DILCopyNodeActionHandler.js` @1,565,684 | 本地动作，**不经服务端** |
| `value_input` | host context（`scope.value_input.affordance(...)`） | 输入控件 affordance |
| `affordance` / `follow_up_action_bar` / `option` | 同上 | 追问栏反馈 |
| `play_requested` / `stop_requested` | `widgets/SpeechSynthesizerWidgetRenderer.js` @224,640 | 语音控件事件 |
| `show_sensitive_image` | 图片相关（64 次） | 敏感图显式放行 |
| `learning_mode_submit` / `widget_action_target` / `widget_action_value` | `src/DILWidgetVoiceAction.js` @1,336,749 | 组件埋点标签 |
| `widget_saves` | `src/DILViewStateSynchronizer.js` | 视图状态持久化（非动作） |
| `file_upload` / `elicitation_type` | `src/DILWidgetMediaUploadAction.js` @1,341,542 | 上传引导状态机 |

---

## 五、门控矩阵

`src/DILHostActionsHandler.js` 的字符串表揭示了一整套前置判定，**每个门控失败都对应一条独立错误串**（🔒 确定）：

| # | 门控维度 | 判定字段 | 失败消息（原文） |
|---|---|---|---|
| 1 | 信任策略 | `getTrustedComponentAppData().securityPolicy` | `client.handler.securityPolicy` |
| 2 | 模型生成标识 | `isModelWrittenDil` / `"model-written-dil"` | `server actions are unavailable to model-written DIL.` |
| 3 | 只读模式 | `dataJSON.isReadOnly` | `actions are unavailable in read-only DIL.` |
| 4 | 非消耗型动作（防重定向） | `isNonConsumingAction` | `payload.action cannot redirect a non-consuming action.` |
| 5 | 降权回退防护 | — | `non-consuming actions cannot fall back to privileged actions.` |
| 6 | 回退深度上限 | `clientActionHandler.fallbackDepth` | `fallback action depth exceeded for type "%s"` |
| 7 | **新鲜用户手势** | `consumeUserInteraction` | `requires a fresh user interaction.` |
| 8 | 插件工具就绪 | `arePluginToolsReady` | `plugin tool results are unavailable in this host.` |
| 9 | 调用消息就绪（服务端动作） | `areRegisteredServerActionsReady` | `the invoking message is not ready for plugin tools.` |
| 10 | 实体动作就绪 | `areEntityActionsReady` / `hostEntityActionsReady` | `the invoking message is not ready for entity actions.` |
| 11 | 实体/服务端数据就绪 | `entityAppDataReady` / `serverAppDataReady` | — |
| 12 | 语音会话就绪 | `widgetVoiceSessionsReady` | `Voice is unavailable for this component.` |
| 13 | 宿主状态事件就绪 | `hostStateEventsReady` | `DIL host-state event registration is unavailable.` |
| 14 | 流式进行中 | `hostMessageIsStreaming` / `isMessageStreaming` | （动作挂起） |
| 15 | 图片审批 | `imageApprovalRequired` | — |
| 16 | 协议版本 | `dilVersion` / `isVersionAtLeast` / `is_legacy_dil_version` / `2.0` | `legacy_v1_render_attempt` |
| 17 | 组件身份 | `componentName` / `nativeWidgetResolutionId` | `Voice is unavailable for this component.` |
| 18 | 动作类型白名单 | `isAllowedModelWrittenDILHostActionType` | `unsupported type "%s".` |
| 19 | 派发前基础校验 | `missing "type".` / `validateClientAction` | `DIL host action.` / `DIL client action.` |

**三处最值得注意**：

1. **第 7 条 = 防脚本化触发**。`consumeUserInteraction` + `requires a fresh user interaction.` —— 特权动作要求**新鲜用户手势**，与 Web 的 transient user activation 同构。
2. **第 4/5/6 条 = 动作链攻击面防护**。禁止非消耗型动作重定向到其他动作、禁止借回退机制提权、限制回退深度，三条合起来封堵"用低权限动作撬动高权限动作"的链式利用。
3. **第 2 条 = 信任分级**。`isModelWrittenDil` 这个标识**同时作用于动作门控与视图状态写回**（见第十节），是最核心的一条信任边界：模型生成的 DIL 与宿主/已注册组件的 DIL 权限不同。

---

## 六、逐动作 payload 校验

全部来自 `DILHostActionsHandler` 字符串表（🔒 确定存在这些校验串）：

| 动作 | 校验规则（原文错误串 / 字段） |
|---|---|
| `open_url` | `canonicalTrustedNavigationURL` / `canonicalHTTPSNavigationURL` / `untrusted URL in payload.url.` |
| `open_file_citation` | `isTrustedFileCitation` / `untrusted file identity in payload.` + 字段 `link_type` `file_id` `file_name` |
| `open_image_lightbox` | `payload.images must be a non-empty array.` / `payload.selected_index is outside payload.images.` / `isInteger` / `canonicalTrustedLightboxURL` / `contains an untrusted url.` / `must contain a trusted content_url or thumbnail_url.` |
| `open_entity_detail` | `every` / `category` / `query` / `readDILModelNamedEntity` / `disambig` / `location` / `address` / `disambiguation` / `extraParams` / `payload.extraParams must be an object when provided.` |
| `request_user_input` | `isArray` / `payload.questions must be a nonempty array.` / `submit_label` / `payload.%s must be a string when provided.` |
| `issue_new_turn` | `issue_new_turn` / `append_to_current_leaf` |
| `log_event` | `event_name` / `statsig_event_name` / `event_data` / `payload.category must be a nonblank string when provided.` |
| `prefill_composer` | `payload.query must be a nonblank string.` |
| `copy_node` | `payload.target_id must be a non-empty string.` / `the compiled DIL renderer is unavailable.` / `target "%s" was not found.` / `the host does not support copy` |
| 通用 | `payload.value must be a nonblank string.` / `must be an object.` / `Invalid %s: %s (%s)` |

**模式**：每个动作都有独立的 payload 结构校验，且**凡涉及外部身份/资源的动作（URL、文件、图片、实体）都额外做"可信性"判定**，而不只做类型校验。

---

## 七、URL / 沙箱 / 文件的三重信任

### 7.1 网络策略（`src/DILTrustedNetworkPolicy.js`，🔍 确证）

| 机制 | 证据 |
|---|---|
| 全局 URL 白名单 | `allowedUrlRegexes`（7 次）/ `isAllowed` / `isAllowedAssetURL` / `setAllowedUrlRegexes` |
| 旧版兼容白名单 | `setLegacyAllowedUrlRegexes`（按 `dilVersion` 区分） |
| 逐元素过滤 | `supportsPerElementAssetURLFiltering` |
| 规范化比对 | `canonicalTrustedNavigationURL` / `canonicalHTTPSNavigationURL` / `canonicalTrustedLightboxURL` |

→ "**先规范化，再按正则白名单判定**"，而非简单前缀匹配。

### 7.2 沙箱路径（`src/DILSandboxDownloadURL.js` @1,412,591，🔒 确定）

```
入口判定： isDILSandboxURL
取路径：   getDILSandboxDownloadPath
前缀白名单： sandbox:
            ./mnt/data/
            ./workspace/
            /root/.codex/skills/remote-skills/      ← 见第十一节
处理链：   .toLowerCase() → startsWith → slice → decodeURIComponent → replace('%25') → some(lastIndexOf)
正则：     ^[A-Za-z0-9][A-Za-z0-9_-]{0,15}$        单段字符集 + 长度 ≤ 16
           :\d+:(?:\s|$)                            拒绝 host:port 形态
           %(?![0-9a-f]{2})                         拒绝非法百分号转义
报错：     unsafe sandbox URL in payload.url.
```

设计要点：**先小写化再匹配**、**先解码再做二次校验（`%25` 重解码）**、**单段长度与字符集受限** —— 三层叠加用于抵抗路径穿越与编码绕过。

### 7.3 复制语义（`src/DILCopyNodeActionHandler.js` @1,565,684 / `DILCopyContentRenderer`）

| 机制 | 证据 |
|---|---|
| 本地执行 | `handleLocalAction`（不经服务端） |
| 宿主能力检查 | `isHostCopySupported` ← `getHostSupportedClientActions()` |
| 目标校验 | `payload.target_id must be a non-empty string.` / `target "%s" was not found.` |
| 渲染器可用性 | `the compiled DIL renderer is unavailable.` |
| 语义完备性 | `Missing copy semantics for DIL renderer "%s".` |
| 复制行为枚举 | `DILCopyImageBehavior`：`Preserve` / `Omit`；imageBehavior：`Container` `Block` `flow` `Transparent` `Inline` `Omit` `Each` |

---

## 八、服务端动作中继

`src/DILRegisteredWidgetAction.js` @1,440,837（🔒 确定）

```
POST /backend-api/widget_server_action
body: { conversation_id, message_id, native_widget_resolution_id, action_type, jsonBody }
```

| 机制 | 证据 |
|---|---|
| **组件身份绑定** | `nativeWidgetResolutionId` + `componentName` + `opGenui componentResults resolved` |
| **注册校验** | `isRegisteredWidgetAction` / `widgetPublicationLifetimeFor`（**发布生命周期**） |
| **分享时效** | `Widget sharing is no longer available.` / `Could not share this widget.` / `Widget sharing is unavailable.` |
| **乐观并发** | `expected_revision` / `quiz.isSafeInteger(expectedRevision)` / `isCurrent` |
| **响应校验** | `RegisteredWidgetResponseWire`（`statusMessage`/`status_message`，nullable）/ `Invalid widget action response.` / `Invalid widget action state.` |
| **动作类型受限** | `serverActionTypes` / `registeredServerActionHandler` / `genui.run_plugin_tool` |

→ 服务端动作必须同时满足：**组件已注册** + **在发布生命周期内** + **版本号匹配** + **动作类型在白名单内** + **调用消息就绪**。

其中 `widgetPublicationLifetimeFor`（发布生命周期）是一个值得单独深挖的机制：分享能力**会随时间失效**，这与"永久可用的动作授权"是不同的安全模型。

---

## 九、逐组件授权（源码级证据）

> 本节全部为 🔒 **确定**级：读到的是一行行明文源码，而非符号表。

这是本次分析中证据最强的一节。`dil_generated_components.js` 内含**未混淆的组件源码**（保留 `//#region <组件名>.dil.tsx` 原始文件名），可直接读出组件如何消费授权结果。

### 9.1 模式 A：宿主授权集 + 硬编码白名单

`src/restricted/components/WhatsNewCapabilityWelcome.generated.js`

```js
//#region WhatsNewCapabilityWelcome.dil.tsx
function WhatsNewCapabilityWelcome(props) {
    const supportedActions = DIL.useAppData((data) => data.opGenui?.supportedClientActions);
    const { state, resolutionComplete } = useResolvedComponentResult("WhatsNewCapabilityWelcome", props);
    if (!state) return resolutionComplete ? props.fallback ?? null : __dil.jsx("loading-block", { width: "100%", height: 88 });
    return __dil.jsx("col", { role: "group", gap: 2, width: "100%", align: "start" },
        state.items.map((item) => {
            const action = state.whats_new_capability_actions[item.action_id];
            const enabled = action != null
                && action.type !== "start_voice_mode"
                && (action.type === "issue_new_turn"
                    || action.type === "open_url"
                    || supportedActions?.includes(action.type));
            ...
```

读出三条规则：

1. **`start_voice_mode` 被显式排除** —— 即使宿主广告支持，该组件也不允许触发语音。
2. **`issue_new_turn` / `open_url` 为组件硬编码无条件允许**。
3. **其余动作必须出现在 `opGenui.supportedClientActions` 中**。

### 9.2 模式 B：授权结果直接决定 UI 是否存在

同文件中的 `WeatherWidgetWithSource`，以及 `StockChart`（`dil_generated_components.js` 中 `supportedClientActions` 共 20 处命中，多个天气/股票类组件同构）：

```js
function WeatherWidgetWithSource(props) {
    const componentKey = DIL.useAppData(...);
    const canShare = DIL.useAppData((appData) => appData.opGenui?.supportedClientActions?.includes("share") === true);
    const { state, resolutionComplete } = useResolvedComponentResult("WeatherWidgetWithSource", props);
    if (!state) return resolutionComplete ? props.fallback ?? null : __dil.jsx("loading-block", { width: "100%", height: 400 });
    return __dil.jsx(ResolvedWeather, {
        componentKey,
        state: canShare ? state : { ...state, share_button_variant: "none" }   // ← 未授权则按钮"不存在"
    });
}
```

**意义**：未授权时不是"点了被拒"，而是**把 `share_button_variant` 覆写成 `none`，让分享入口根本不存在**。这是纵深防御的 UI 侧实现，也是"授权集"与"界面"绑定在一处的直接证据。

### 9.3 组件分级

组件按目录分为两档（见 `dil_generated_components.js` 的模块路径）：

```
src/components/...                     普通组件
src/restricted/components/...          受限组件（WhatsNewCapabilityWelcome / WeatherWidgetWithSource / StockChart ...）
src/DILComponentSources.generated.js   → 聚合 Public + Restricted 两套来源
```

`DILRestrictedComponentSources.generated.js` 与 `DILPublicComponentSources.generated.js` 分开打包，说明**受限组件走单独的授权档位**。

---

## 十、配套的视图状态写回

`src/DILViewStateSynchronizer.js`（🔍 确证）—— 与动作体系配套的写回通道：

```
POST /backend-api/conversation/{id}/message/{id}/dil/view_state
字段: widget_saves / client_update_id / client_session_id / scope / ref_id / updates
```

| 机制 | 证据 |
|---|---|
| 本地优先 | `recentLocalRootStates` / `rememberUpdateId` |
| 去抖 + 重试 | `scheduleFlush` / `debounceTimerId` / `failedPersistenceAttempts` / `attempt %d/5` |
| 永久失败熔断 | `Stopped persisting DIL view state after a permanent failure` |
| 服务端错误码 | `Unavailable` / `InvalidRequest` / `Cancelled` / `HostRejected` / `SerializationError` / `ServiceError` |
| 消息失效 | `dil_view_state_message_not_found` + `detail.code` / `retryable` |
| 模型生成标识 | `isModelWrittenDil` —— **与动作门控共用同一标识** |

> `isModelWrittenDil` 同时出现在动作授权与状态写回两条链路，是本体系最重要的信任边界。

---

## 十一、值得单独关注的几点

### 11.1 ⚠️ Android 端的沙箱前缀里出现 Codex 服务端路径

沙箱白名单中包含 **`/root/.codex/skills/remote-skills/`** —— 这是 **Codex（服务端 agent）沙箱的路径**，却出现在 ChatGPT **移动端**的路径白名单中。

- 🔍 确证：该字符串确实存在于 `src/DILSandboxDownloadURL.js` 的前缀判定表中。
- 💡 推断：移动端与服务端共享同一份沙箱路径判定代码（或同一跨端共享库），即移动端的"沙箱"语义与 Codex 服务端沙箱同源。
- ❓ 未知：客户端本地是否真的存在该路径、该分支在移动端是否可达。**建议单独深挖** —— 若该路径在移动端可达，则"沙箱路径"这一信任假设的适用范围需要重新评估。

### 11.2 `src/DILActions.js` 已被完全 tree-shake

该模块的字符串表只剩 `__esModule` 与一个内容 hash（`a3a5ce8e9c6478fbf50ae54e08cdbe102553269ca555a016ca7d0c140f3af05d`），无任何导出符号 —— 说明动作注册已**全部内联进 `DILHostActionsHandler`**，`DILActions` 只是遗留的 barrel 文件。

### 11.3 `DIL` 的展开未知

全 bundle 搜索 `Declarative` / `Interface Language` / `Dynamic Interface` / `DIL stands` **均无命中**。❓ 未知。

### 11.4 敏感内容放行是"显式动作"

`show_sensitive_image` 出现在 i18n 文案中（64 次），描述为"移除敏感图预览模糊的按钮的无障碍标签"，即**模糊→清晰需要用户显式动作**，不是自动解密。与第 7 条"新鲜用户手势"一致。

---

## 十二、复现步骤与产物

**输入**：`d:\MyApplication\com.openai.chatgpt_1791789377928.apk`

**工作目录**：`%TEMP%\cgpt\`（提取自 APK 的 DIL bundle + 分析脚本）

| 脚本 | 作用 |
|---|---|
| `modules.py` | 按模块边界切分 `dil_widgets.js`，dump 8 个动作相关模块的字符串表 |
| `action_caps.py` | 提取动作系统多窗口上下文 |
| `snake.py` | 枚举全部 snake_case 标识符（动作名/能力/字段） |
| `actions2.py` | 能力常量全集 + 跨 bundle 完整性校验 + 解析函数上下文 |
| `gen_actions.py` | 从生成组件中提取逐组件授权的**真实源码** |
| `probe_fmt.py` / `format_check.py` | 判定 bundle 容器格式与内容可读性 |

**产物（ASCII 净化，可直接检索）**：

```
out_modules.txt       45 KB   8 个动作模块的完整字符串表
out_action_caps.txt   82 KB   动作系统上下文
out_snake.txt         23 KB   392 个 snake_case 标识符 + 频次
out_actions2.txt      26 KB   能力常量全集与解析函数上下文
out_gen_actions.txt   16 KB   逐组件授权源码片段（🔒）
out_probe_fmt.txt      5 KB   容器格式抽样
```

**关键偏移（`dil_widgets.js` 内）**：

```
1,336,749  src/DILWidgetVoiceAction.js
1,341,542  src/DILWidgetMediaUploadAction.js
1,412,591  src/DILSandboxDownloadURL.js
1,440,837  src/DILRegisteredWidgetAction.js
1,468,142  src/DILHostActionsHandler.js     ← 授权核心
1,565,684  src/DILCopyNodeActionHandler.js
1,619,901  src/DILClientActionCapabilities.js ← 能力常量定义处
1,624,042  src/DILActions.js（已 tree-shake）
```

---

## 十三、本次分析的边界

1. **纯静态**：未运行 APK、未连接服务、未使用 Frida/调试器，所有结论来自文件字节。
2. **`dil_widgets.js` 只能读符号表**：动作系统的**实现体**不可读，因此"门控的实际执行顺序""能力与派生动作的精确配对""各门控的与/或关系"属 💡 推断。
3. **未验证运行时行为**：第 5 节门控表中，哪些是硬阻断、哪些只降级 UI，未做运行时确证。
4. **未展开全部组件**：`supportedClientActions` 在生成组件中命中 20 处，本次只读了 3 处代表（`WhatsNewCapabilityWelcome` / `WeatherWidgetWithSource` / `StockChart`），**未产出全组件总表**。
5. **未与其他 bundle 交叉验证**：`dil_core.js` / `dil_renderer.js` / `dil_compiler.js` 侧的对应实现未读。

---

## 十四、未完成的方向

| # | 方向 | 价值 |
|---|---|---|
| A | `DILTrustedNetworkPolicy` 白名单机制（正则白名单如何构建、`setLegacyAllowedUrlRegexes` 旧版兼容风险） | 高（URL 信任是外链类动作的总闸） |
| B | `DILFlexContainerAutoSpacing` 自动间距算法 | 中（渲染机制，非安全） |
| C | **运行时验证**：Frida hook `dispatchClientAction`，确证门控实际生效顺序与"新鲜手势"条件 | 高（把 💡 提升为 🔒） |
| D | **全组件动作授权总表**：展开 `dil_generated_components.js`，产出"组件 → 被授权动作"清单 | 高（直接回答"授权集是什么"） |
| E | 深挖 `widgetPublicationLifetimeFor` / 发布生命周期与 `/root/.codex/...` 路径 | 中高（两处语义异常） |

---

*本文由静态逆向分析产出，记录于 2026-10-12。所有 🔒 级结论均可用第十二节的脚本复现。*
