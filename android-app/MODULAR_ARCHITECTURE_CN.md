# SwiftPDF Android 模块化架构规划

## 核心原则

SwiftPDF 后续会包含 PDF 阅读、扫描、OCR、导出、压缩、签名、订阅等能力。为了避免后期模块互相耦合，当前先采用“单 Gradle module，多业务包边界”的方式开发；等功能稳定后，再平滑拆成多个 Gradle module。

依赖方向固定为：

```text
app shell / navigation
-> feature/*
-> domain/*
-> data/* / services/*
-> Android framework / third-party SDK
```

UI 不直接读取文件系统，不直接调用 PDF SDK，不直接处理持久化。UI 只消费状态并发出事件。

## 当前阶段包结构

```text
com.swiftpdf.app
  navigation
  ui
    components
    screens
    theme
  domain
    document
  data
    document
  feature
    library
```

## 目标阶段包结构

```text
com.swiftpdf.app
  app
    AppGraph
    MainActivity
  navigation
    Routes
    AppNavGraph
  core
    model
    result
    permissions
    dispatchers
  domain
    document
    reader
    scan
    export
    pro
  data
    document
    preferences
    history
  services
    pdf
    image
    ocr
    share
    billing
  feature
    home
    reader
    scan
    export
    settings
    onboarding
    pro
  ui
    components
    theme
```

## 后续 Gradle module 拆分路线

第一阶段先保持单 module，原因是速度快、调试简单、功能还在探索。

当 Reader 和文件历史稳定后，再拆：

```text
:app
:core:model
:core:ui
:domain:document
:data:document
:feature:home
:feature:reader
:services:pdf
```

扫描、OCR、导出稳定后继续拆：

```text
:feature:scan
:feature:export
:services:image
:services:ocr
:services:billing
```

## 模块职责

### navigation

- 管理路由。
- 只连接 feature screen，不写业务逻辑。
- 不直接访问 Android 文件系统、PDF SDK、数据库。

### domain/document

- 定义 `DocumentItem`。
- 定义 `DocumentRepository` 接口。
- 后续增加 use case，例如 `ImportDocumentUseCase`、`OpenDocumentUseCase`、`RemoveRecentDocumentUseCase`。

### data/document

- 实现 Android 侧文件访问。
- 处理 Storage Access Framework、URI 权限、文件名和大小读取。
- 后续接 Room/DataStore 持久化。

### feature/library

- 管理最近文件和当前选中文件状态。
- 对 UI 暴露 `DocumentLibraryUiState`。
- 后续迁移为 `HomeViewModel` 或 `DocumentLibraryViewModel`。

### feature/reader

- 只负责 Reader 状态和用户动作。
- 不直接做 PDF 渲染，渲染通过 `PdfRendererService` 接口完成。

### services/pdf

- 后续封装 `PdfRenderer` 或第三方 PDF SDK。
- 对外提供打开、页数、渲染 bitmap、关闭文档等能力。

## 当前代码解耦目标

本轮先完成：

- 把 `RecentDocument` 升级为 domain 层 `DocumentItem`。
- 把 Android 文件元信息读取移到 `AndroidDocumentRepository`。
- 把最近文件和当前文件状态移到 `DocumentLibraryViewModel`。
- 让 `SwiftPdfApp` 只负责导航和调用 ViewModel。

下一轮继续：

- 给 `DocumentLibraryViewModel` 增加持久化。
- 把 Home / Reader 移入 `feature/home` 和 `feature/reader` 包。
- 增加 `PdfRendererService` 接口，开始 Phase 2 PDF 首页渲染。
