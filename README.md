# MINI ClassIsland — Android 移动端/平板集控客户端

<p align="center">
  <a href="https://miniclassisland.com" target="_blank" rel="noopener noreferrer">
    <img src="assets/miniclassisland.png" alt="MINI ClassIsland Logo" width="128" height="128" />
  </a>
</p>


**MINI ClassIsland Android**（包名：`org.miniopensource.miniclassisland`）是基于 **Jetpack Compose + Material 3 Adaptive** 构建的原生 Android 集控与治理客户端。它同时集成了面向学校/机构管理员的 **租户集控工作台（Management API `:27042`）** 与面向平台超级管理员的 **全局治理中心（Admin API `:27043`）**。

---

## 核心特性

### 1. 自适应多形态布局（Adaptive Phone & Tablet UX）
- **手机端（Compact `< 600dp`）**：单栏紧凑布局 + 底部导航栏 + 侧滑抽屉菜单，方便教务人员随时随地巡检设备、发送紧急通知与审批配对。
- **平板 / 折叠屏（Medium `>= 600dp` / Expanded `>= 840dp`）**：常驻侧边导航栏（`NavigationRail`）+ 双栏主从分屏视图（Master-Detail Split View），支持在左侧浏览资源或终端列表的同时在右侧编辑课表或查看终端快照。

### 2. 租户集控功能（Management API `:27042`）
- **认证与引导**：支持自定义服务器地址、OOBE 首次初始化向导、账号登录/注册、TOTP 二步验证与恢复码验证。
- **总览与接入配置**：实时查看本租户在线终端率、配额进度，一键生成并分享 `ManagementPreset.json`。
- **7 类配置资源可视化与 JSON 双模编辑**：
  - `Subjects`（科目）、`TimeLayout`（时间表）、`ClassPlan`（课表）、`Policy`（策略）、`DefaultSettings`（默认设置）、`Components`（组件）、`Credentials`（凭据）。
- **终端与命令下发**：
  - 查看终端在线状态、IP、分组与绑定方案。
  - 下发单机或全校广播通知（支持普通横幅、紧急全屏遮罩、语音播报、音效与特效开关）。
  - 远程触发终端重启（`RestartApp`）、强制同步（`DataUpdated`）、远程抓取 1,000 条运行日志或配置快照（`GetClientConfig`）。
- **设备准入控制**：支持开放接入（`open`）、6 位配对码审批（`pairing`）、预注册白名单（`pre_reg`）。
- **租户组织管理**：成员权限分配、邀请码签发、配额监控、操作审计日志与个人安全中心。

### 3. 超级管理员全局治理（Admin API `:27043`）
- **全局监控看板**：实时展示全平台用户数、租户数、终端总数与实时在线率。
- **租户与配额治理**：查看并动态调整任意租户的 7 项配额上限，支持一键跨租户接管调试。
- **用户与注册审批**：审批待激活用户、封禁违规账号、调整全局注册策略与默认配额模板。

---

## 技术栈与工程结构

- **语言与工具链**：Kotlin 2.0+、JDK 17、Android Gradle Plugin (AGP)
- **UI 框架**：Jetpack Compose、Material 3、Material 3 Adaptive Navigation Suite
- **网络与序列化**：Retrofit 2 / OkHttp 4、Kotlinx Serialization

```text
android/
├── app/
│   ├── src/main/
│   │   ├── java/org/miniopensource/miniclassisland/
│   │   │   ├── api/            # Management (:27042) 与 Admin (:27043) API 客户端与数据模型
│   │   │   ├── data/           # 本地持久化存储 (DataStore / SharedPreferences) 与会话状态
│   │   │   ├── ui/             # Jetpack Compose 屏幕、自适应导航骨架与编辑器组件
│   │   │   └── MainActivity.kt # 应用入口
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
└── build.gradle.kts
```

---

## 本地构建与运行

```bash
cd android

# 编译 Debug APK
./gradlew assembleDebug

# 运行单元测试与 Lint 检查
./gradlew testDebugUnitTest lintDebug
```

编译产物位于 `app/build/outputs/apk/debug/app-debug.apk`。
