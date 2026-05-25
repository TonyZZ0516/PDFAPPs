# SwiftPDF Android 开发拆分

## 目标

先把 Android App 做成可运行、可演进的 MVP。每一阶段都应该能独立验证，避免一次性堆太多 SDK 和复杂能力。

## Phase 0：工程基础

状态：已开始。

- 建立 `android-app/` 工程目录。
- 使用 Kotlin + Jetpack Compose + Material 3。
- 建立 `Home / Reader / Scan / Export / Settings` 底部导航。
- 建立主题、通用卡片、页面占位。
- 后续补充 Gradle Wrapper、基础单元测试和 CI 检查。

## Phase 1：首页和文件导入

状态：已启动，当前完成系统 PDF 选择器、DataStore 持久化最近文件列表、点击最近文件进入 Reader、从最近文件移除记录。

- 接入 Android document picker。
- 建立 `DocumentItem` 数据模型。
- 建立最近文件列表。
- 支持空状态、导入来源弹窗、文件元信息展示。
- 支持从最近文件移除记录，不删除用户原始 PDF。
- 本阶段使用 DataStore 做轻量持久化，暂不做云同步。

## Phase 2：PDF 阅读器

状态：已启动，当前完成 `PdfRendererService` 接口、Android `PdfRenderer` 实现、Reader 指定页渲染、上一页/下一页翻页。

- 选择 PDF 渲染方案：优先 Android `PdfRenderer`，复杂能力后续再评估第三方库。
- 支持打开单个 PDF。
- 支持页码、缩略图入口、搜索入口占位。
- 支持分享、导出入口。
- 本阶段重点验证大文件性能和页面滚动体验。

## Phase 3：扫描生成 PDF

状态：已启动，当前完成 Scan 页面骨架、相机权限 pre-prompt、系统相机权限请求、授权/拒绝状态展示、CameraX 真实预览、拍照保存到缓存目录、扫描页列表、移除扫描页、导出扫描页为 PDF 并进入 Reader。

- 接入 CameraX。
- 做相机权限 pre-prompt 和系统权限处理。
- 实现拍照、预览、重拍、保存。
- 边缘检测和自动裁切先保留接口，MVP 可先手动确认。
- 输出 PDF 并回到 Reader 或 Home。

## Phase 4：OCR 和导出

- 导出/分享状态：已启动，当前完成当前 PDF 分享、FileProvider 安全分享、扫描页导出 PDF 后进入 Reader。

- 定义 OCR service 接口，先用 mock 或本地占位实现。
- 支持导出图片、文本、压缩 PDF 的流程壳。
- 实现导出进度弹窗、完成状态、错误状态。
- Pro 限制先做前端触发点，不急着接支付。

## Phase 5：存储和历史

- 引入 Room 或 DataStore 保存最近文件、扫描记录、导出记录。
- 统一文件访问权限和 URI 持久授权。
- 增加文件删除、重命名、排序、筛选。
- 设置页已支持最近文件数量展示和清空历史。

## Phase 6：商业化和发布准备

- 接入订阅状态模型。
- 接入支付前先完善 Pro feature gate。
- 增加隐私政策、权限说明、崩溃日志策略。
- 准备 Play Console 发布资料。

## 建议开发顺序

1. Phase 0 跑通空壳 App。
2. Phase 1 做真实文件导入。
3. Phase 2 做 PDF 阅读器最小闭环。
4. Phase 3 做扫描闭环。
5. Phase 4 以后再进入 OCR、导出、压缩和 Pro。

## 当前下一步

用 Android Studio 打开 `android-app/`，完成 Gradle Sync。如果本机缺 JDK 或 Android SDK，先在 Android Studio 里安装对应环境，再生成 Gradle Wrapper。
