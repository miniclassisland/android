# MINI ClassIsland Android 贡献指南 (Contributing Guide)

感谢你对 `miniclassisland/android` 项目的关注与贡献！在提交代码前，请阅读并遵守以下开发规范。

---

## 1. 开发环境准备

- **Android Studio**：Ladybug (2024.2.1) 或更新版本
- **JDK**：JDK 17
- **Android SDK**：`compileSdk 35`、`minSdk 26`

---

## 2. 架构与编码规范

### 2.1 自适应布局规范（Phone & Tablet）
- 所有核心业务页面必须同时适配紧凑型屏幕（`< 600dp`）与展开型大屏（`>= 600dp` / `>= 840dp`）。
- 列表-详情类页面（如 7 类配置资源编辑、终端列表与详情抽屉）在大屏模式下应采用双栏分屏展示，避免在大屏上直接拉伸单列卡片。
- 状态提升（State Hoisting）：Compose 组件应保持无状态或仅持有局部 UI 状态，网络请求与鉴权上下文统一通过状态容器管理。

### 2.2 API 与上游模型对齐
- 请求头必须携带当前选中的租户上下文（`X-Tenant-ID` / `X-Tenant-Slug`）与 `Authorization: Bearer <token>`。
- 7 类配置资源的序列化字段必须严格保持与上游 ClassIsland C# 模型一致的 `PascalCase` 命名，禁止重命名或丢失未知扩展字段。

### 2.3 安全与 `.gitignore` 规范
- 严禁将签名密钥库（`*.jks`、`*.keystore`）、私钥（`*.pem`、`*.key`、`*.pfx`）、`local.properties` 或硬编码的测试服务器凭据提交至仓库。

---

## 3. 提交前质量检查

在提交 Pull Request 前，请确保通过以下构建与检查：

```bash
./gradlew assembleDebug
./gradlew lintDebug
./gradlew testDebugUnitTest
```

---

## 4. Commit 规范与 SSH 签名

- 遵循 **Conventional Commits** 规范（如 `feat(android): ...`、`fix(ui): ...`、`docs(readme): ...`）。
- 所有 Commit 建议使用 GitHub 登记的 SSH 密钥签名（`git commit -S`）。
