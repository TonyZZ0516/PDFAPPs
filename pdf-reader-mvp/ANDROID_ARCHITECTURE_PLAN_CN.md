# SwiftPDF Android MVP 架构与开发拆分

更新时间：2026-05-22

## 1. 开发目标

SwiftPDF 第一版 Android App 的目标是先完成一个可发布的 MVP 闭环：

```text
首次引导 -> 首页 -> 导入/打开 PDF -> 阅读 -> 工具处理 -> 导出副本 -> 打开/分享 -> 回到首页
```

第一阶段不追求完整 PDF Office，而是优先完成这些高频能力：

- 本地 PDF 文件发现、导入、打开。
- PDF 阅读器。
- 图片转 PDF。
- 签名并保存副本。
- 压缩、合并、拆分、PDF 转图片的入口和基础流程。
- 导出成功、失败恢复、权限说明、Pro 限制状态。

## 2. 推荐技术栈

- Android 原生。
- Kotlin。
- Jetpack Compose。
- Material 3。
- Navigation Compose。
- ViewModel + Kotlin Flow。
- DataStore 保存用户偏好。
- Room 可后置，MVP 初期可先用轻量本地索引。
- Storage Access Framework 处理用户选择文件。
- PdfRenderer 或成熟 PDF 渲染库处理阅读。
- CameraX 后续接入拍照导入。
- Billing Library 后续接入订阅。

## 3. 项目目录建议

建议在仓库下新建真实 Android 工程：

```text
android-app/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/swiftpdf/app/
│     │  ├─ SwiftPdfApp.kt
│     │  ├─ MainActivity.kt
│     │  ├─ navigation/
│     │  ├─ ui/
│     │  ├─ domain/
│     │  ├─ data/
│     │  ├─ services/
│     │  └─ billing/
│     └─ res/
└─ design/
```

第一版可以先做单 module：`app`。等功能稳定后再拆多 module。

## 4. 分层架构

```text
UI Layer
├─ Compose Screen
├─ Reusable Components
└─ ViewModel State

Domain Layer
├─ Use Cases
├─ Models
└─ Validation Rules

Data / Service Layer
├─ File Repository
├─ PDF Renderer
├─ PDF Processor
├─ Image Importer
├─ Signature Store
├─ Export Manager
└─ Billing Gate
```

### UI Layer

只负责界面展示和用户事件：

- `HomeScreen`
- `ReaderScreen`
- `ToolboxScreen`
- `ImageToPdfScreen`
- `SignatureCreateScreen`
- `SignaturePlaceScreen`
- `ExportResultScreen`
- `ProLimitSheet`
- `PermissionPromptScreen`

### Domain Layer

负责业务规则：

- 文件是否可打开。
- 编辑是否必须保存副本。
- 每日免费导出次数是否达到限制。
- 导出前是否需要 Pro。
- 当前任务是否可恢复。

### Data / Service Layer

负责 Android 具体能力：

- 文件选择。
- URI 权限持久化。
- PDF 渲染。
- PDF 生成。
- 图片读取和裁剪。
- 签名图片叠加。
- 分享 Intent。
- 本地偏好存储。

## 5. Package 结构建议

```text
com.swiftpdf.app
├─ navigation
│  ├─ AppNavGraph.kt
│  └─ Routes.kt
├─ ui
│  ├─ theme
│  ├─ components
│  ├─ home
│  ├─ reader
│  ├─ toolbox
│  ├─ imagepdf
│  ├─ signature
│  ├─ export
│  ├─ pro
│  └─ onboarding
├─ domain
│  ├─ model
│  ├─ usecase
│  └─ gate
├─ data
│  ├─ files
│  ├─ preferences
│  └─ history
├─ services
│  ├─ pdf
│  ├─ image
│  ├─ signature
│  └─ share
└─ billing
```

## 6. 页面路由

```kotlin
sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Home : Route("home")
    data object Reader : Route("reader/{documentId}")
    data object Toolbox : Route("toolbox")
    data object ImageToPdf : Route("image-to-pdf")
    data object ImagePreview : Route("image-preview")
    data object SignatureCreate : Route("signature-create")
    data object SignaturePlace : Route("signature-place/{documentId}")
    data object ExportProgress : Route("export-progress/{taskId}")
    data object ExportResult : Route("export-result/{fileId}")
    data object ProLimit : Route("pro-limit/{trigger}")
}
```

弹窗/Bottom Sheet 不一定都做成 route，可以由当前 Screen 管理：

- `ImportSourceSheet`
- `SortFilterSheet`
- `RenameDialog`
- `DeleteConfirmDialog`
- `CompressSettingsSheet`
- `PermissionPrePrompt`
- `SearchOverlay`

## 7. 核心状态模型

### DocumentItem

```kotlin
data class DocumentItem(
    val id: String,
    val displayName: String,
    val uri: Uri,
    val pageCount: Int?,
    val sizeBytes: Long?,
    val lastOpenedAt: Instant?,
    val isBookmarked: Boolean,
    val source: DocumentSource
)
```

### ExportTask

```kotlin
data class ExportTask(
    val id: String,
    val type: ExportType,
    val status: ExportStatus,
    val progress: Float,
    val inputUris: List<Uri>,
    val outputUri: Uri?,
    val errorMessage: String?
)
```

### ProTrigger

```kotlin
enum class ProTrigger {
    IMAGE_TO_PDF_DAILY_LIMIT,
    PDF_TO_IMAGE_HD,
    HIGH_QUALITY_COMPRESS,
    BATCH_MERGE_SPLIT,
    SIGNATURE_LIBRARY
}
```

## 8. 数据流

### 打开 PDF

```text
HomeScreen
-> user taps file
-> HomeViewModel.openDocument(id)
-> FileRepository.resolveDocument(id)
-> Reader route
-> ReaderViewModel loads page metadata
-> PdfRendererService renders visible pages
```

### 图片转 PDF

```text
ImageToPdfScreen
-> ImportSourceSheet
-> gallery/camera/local input
-> selected image list
-> reorder/crop
-> ImagePreview
-> ExportManager.createPdfFromImages()
-> ExportProgress
-> ExportResult
```

### 签名 PDF

```text
ReaderScreen
-> Sign action
-> SignatureCreateScreen
-> SignaturePlaceScreen
-> PdfProcessor.applySignatureAsCopy()
-> ExportResult
```

### Pro 限制

```text
User requests premium operation
-> ProGate.check(trigger)
-> if allowed: continue
-> if blocked: ProLimitSheet
-> Continue free or Start Pro
```

## 9. 核心服务拆分

### FileRepository

职责：

- 保存最近文件索引。
- 记录最近打开时间。
- 持久化用户选择的 URI 权限。
- 提供搜索、排序、重命名展示名、删除记录。

注意：

- MVP 不要真实删除用户原始文件，除非用户明确授权。
- 删除动作第一版可先表示“从最近文件移除”。

### PdfRendererService

职责：

- 打开 PDF。
- 获取页数。
- 渲染指定页 bitmap。
- 支持缩放、分页、缓存。

风险：

- 大 PDF 内存占用。
- 加密 PDF。
- 损坏 PDF。
- Android 版本差异。

### PdfProcessorService

职责：

- 图片生成 PDF。
- 签名叠加保存副本。
- 压缩 PDF。
- 合并 PDF。
- 拆分 PDF。
- PDF 转图片。

MVP 建议：

- 第一轮只真实实现图片转 PDF 和签名保存副本。
- 压缩、合并、拆分、PDF 转图片可先做 UI 和任务骨架，再逐个接真实处理。

### ImageImportService

职责：

- 相册多选。
- 相机导入。
- 图片 URI 读取。
- 基础尺寸检查。
- 后续接裁剪。

### SignatureService

职责：

- 手写签名画布。
- 签名透明背景导出。
- 放置位置记录。
- 叠加到 PDF 页面。
- 签名库后续作为 Pro 能力。

### ExportManager

职责：

- 管理导出任务。
- 输出进度。
- 成功文件注册。
- 失败恢复。
- 分享 Intent。

## 10. 组件拆分

### 通用组件

- `SwiftPdfLogo`
- `AppTopBar`
- `IconButton`
- `PrimaryButton`
- `SecondaryButton`
- `ToolCard`
- `QuickToolCard`
- `FileRow`
- `EmptyState`
- `SuccessState`
- `ErrorState`
- `ProBadge`
- `ProgressBar`
- `BottomActionBar`

### 业务组件

- `PdfPageCanvas`
- `ReaderBottomBar`
- `ImageThumbGrid`
- `SignaturePad`
- `SignatureOverlay`
- `ExportProgressPanel`
- `ProLimitSheet`
- `ImportSourceSheet`
- `SortFilterSheet`

## 11. 开发阶段拆分

### Phase 0：工程初始化

目标：

- 创建 Android 工程。
- 配置 Kotlin、Compose、Material 3、Navigation。
- 建立主题色、字体、基础组件。

交付：

- App 可启动。
- 可进入 Home 空页面。
- 有基础导航框架。

### Phase 1：静态页面复刻

目标：

- 按 Figma 复刻主页面和关键弹窗。

范围：

- Onboarding。
- Home。
- Reader mock。
- Toolbox。
- Image to PDF mock。
- Signature mock。
- Export Result。
- Pro Limit。
- Flows + Dialogs 里的关键弹窗。

交付：

- 所有页面可点击跳转。
- 暂不接真实文件能力。

### Phase 2：本地文件导入与首页列表

目标：

- 接入 Storage Access Framework。
- 支持选择 PDF。
- 首页展示最近文件。
- 搜索、排序、重命名展示名、移除记录。

交付：

- 用户可以导入 PDF。
- App 重启后仍能看到最近文件。

### Phase 3：PDF 阅读器

目标：

- 打开 PDF。
- 渲染页面。
- 显示页码。
- 基础翻页/滚动。

交付：

- 从首页打开 PDF 到 Reader。
- 失败时展示错误恢复状态。

### Phase 4：Image to PDF

目标：

- 相册选择图片。
- 排序。
- 预览。
- 导出 PDF。

交付：

- 图片可以生成 PDF。
- 导出成功后出现在最近文件。
- 可分享导出文件。

### Phase 5：签名保存副本

目标：

- 手写签名。
- 放置到 PDF。
- 保存为副本。

交付：

- 不覆盖原 PDF。
- 新文件可打开、分享。

### Phase 6：工具箱扩展

目标：

- 压缩、合并、拆分、PDF 转图片逐个接真实能力。

策略：

- 先做每个工具的任务框架。
- 再按风险从低到高接真实处理。

### Phase 7：Pro Gate 与订阅

目标：

- 接入 Pro 触发点。
- 接入 Billing Library。
- 支持恢复购买。

交付：

- 免费限制可配置。
- Pro 用户绕过限制。

### Phase 8：Polish 与发布准备

目标：

- 错误状态补齐。
- 性能优化。
- 大文件测试。
- Android 权限文案检查。
- Google Play 上架素材准备。

## 12. MVP 优先级

### P0 必须做

- 工程骨架。
- Home。
- 文件导入。
- PDF 阅读。
- Image to PDF。
- 导出成功/失败。
- 分享。
- 基础权限说明。

### P1 建议第一版做

- 签名保存副本。
- 搜索。
- 排序。
- 重命名展示名。
- Pro 限制 UI。

### P2 可后置

- 压缩真实实现。
- 合并真实实现。
- 拆分真实实现。
- PDF 转图片真实实现。
- CameraX。
- 签名库。
- Billing 真实支付。
- OCR。

## 13. 主要风险

- PDF 渲染性能和内存。
- Android 文件 URI 权限处理。
- 不同来源文件的持久访问。
- 大图片转 PDF 的内存和耗时。
- 签名叠加后 PDF 兼容性。
- 真实压缩/合并/拆分库的许可证和稳定性。
- Google Play 对订阅、文件权限、隐私说明的审核要求。

## 14. 下一步行动

建议下一步直接进入：

```text
Phase 0：创建 android-app 工程
```

具体任务：

1. 创建 Android Gradle 工程。
2. 配置 Compose + Material 3。
3. 建立主题 tokens。
4. 建立 Navigation。
5. 先实现 Home / Reader / Toolbox 三个静态页面。
6. 跑通本地 debug build。

完成 Phase 0 后，再进入 Phase 1，把 Figma 里的主页面和弹窗逐步复刻到 Compose。
