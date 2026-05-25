# SwiftPDF 项目需求与完成情况整理

更新时间：2026-05-24

## 1. 产品目标

SwiftPDF 的目标是做一个 Android 端 PDF 工具 App，先完成 MVP，然后逐步扩展为可发布的移动端 PDF 处理产品。

当前产品定位：

- 面向手机用户的 PDF 阅读、转换、签名、压缩、合并、拆分工具。
- 优先保证核心流程可用、页面风格统一、模块松耦合，便于后续继续开发。
- 当前 UI 使用英文界面文案，适配移动端竖屏使用。

## 2. 当前核心需求

### 首页 Home

- 展示品牌和最近文档。
- 支持搜索 PDF、工具或文件夹。
- 展示快捷工具入口：
  - Image to PDF
  - Sign PDF
  - Compress
  - Merge
  - Split
  - PDF to Image
- 展示 Recent files。
- 支持最近文件排序、打开、更多操作。

### 阅读页 Reader

- 点击单个 PDF 后进入阅读页面。
- 阅读页面改为竖向连续阅读，而不是只显示单页翻页预览。
- PDF 页面按需渲染并缓存，避免重复渲染。
- 针对文档编辑，只保留 6 个工具入口：
  - Search
  - Bookmark
  - Sign
  - Night
  - Note
  - Share
- 已移除阅读页上的非编辑控件：
  - 顶部重复文件信息卡片
  - 上一页 / 下一页进度条
  - Zoom - / Reset / Zoom +
  - 页码输入和 Go

### Scan / Image to PDF

- 支持相机权限流程。
- 支持图片导入。
- 支持图片队列、旋转、排序、删除、重新拍摄。
- 支持将图片导出为 PDF。

### Export / PDF 工具箱

- 支持 PDF to Image。
- 支持 Sign PDF。
- 支持 Compress。
- 支持 Merge。
- 支持 Split。
- 支持导出成功后的结果操作。
- 支持导出失败后的恢复操作，例如 Save draft / Retry。
- 支持 Pro 弹窗和继续免费路径。

### Settings

- 展示最近文件状态。
- 展示存储、质量、版本等基础说明。
- 预留后续订阅、权限、隐私、缓存等设置扩展空间。

## 3. 已完成的主要工作

### 产品与设计

- 已完成产品流程梳理。
- 已完成高保真 HTML 设计稿：
  - `pdf-reader-mvp/HI_FI_DESIGN_CN.html`
- 已完成设计规格文档：
  - `pdf-reader-mvp/DESIGN_SPEC_CN.md`
- 已完成 Android 架构规划：
  - `pdf-reader-mvp/ANDROID_ARCHITECTURE_PLAN_CN.md`
- 已完成 Android 开发拆分规划：
  - `android-app/DEVELOPMENT_SPLIT_CN.md`
- 已完成测试方案：
  - `android-app/APP_TEST_PLAN_CN.md`
  - `android-app/FUNCTIONAL_TEST_PLAN_CN.md`

### Android 工程

- 已创建并运行 Android 工程：
  - `android-app/`
- 已完成底部导航：
  - Home
  - Reader
  - Scan
  - Export
  - Settings
- 已实现模块化基础结构：
  - `domain`
  - `data`
  - `feature`
  - `services`
  - `ui`
  - `navigation`
- 已实现 PDF 渲染、PDF 工具服务、图片转 PDF、分享服务等基础服务层。
- 已实现 DataStore 最近文件历史。

### 最近完成的 Reader 调整

- Reader 已从单页阅读改成竖向连续阅读。
- Reader 状态从单个 `preview` 改为多页 `pagePreviews` 缓存。
- Reader 支持页面按需渲染。
- Reader 工具栏已精简为只保留 6 个文档编辑相关功能。
- 已重新打包并安装到模拟器验证。

## 4. 当前测试状态

### 单元测试

最近一次测试命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --stacktrace
```

结果：

- 测试总数：42
- Failures：0
- Errors：0
- Debug APK 构建：通过

### 模拟器测试

当前模拟器：

- `emulator-5554`

已验证：

- App 可安装。
- App 可启动。
- 首页可显示最近文件。
- 点击 PDF 可进入 Reader。
- Reader 可显示竖向 PDF 页面。
- 2 页 PDF 可从 Page 1 滑动到 Page 2。
- Reader 工具栏已只保留 Search / Bookmark / Sign / Night / Note / Share。
- logcat 未发现 `FATAL EXCEPTION` / ANR。

最新 Reader 精简截图：

- `android-app/test-reports/reader_edit_toolbar_trimmed.png`

### 已完成的较完整验收

之前已完成完整自动化验收：

- `android-app/test-reports/automated_2026-05-23_220823/verification_report.md`

之前已完成像素和兼容性验收：

- `android-app/test-reports/pixel_acceptance_2026-05-23/README.md`

之前验收结果：

- 兼容截图：23/23 passed
- Render gate：23/23 passed
- 目标流程：11/11 passed
- 设计覆盖：12 pass / 0 gap

说明：Reader 最新精简改动已完成单元测试、打包和模拟器验证，但尚未重新跑完整像素级验收全集。

## 5. 当前 APK 输出

Debug APK：

- `android-app/app/build/outputs/apk/debug/app-debug.apk`

Release APK：

- `android-app/app/build/outputs/apk/release/app-release.apk`

说明：最新安装到模拟器的是 Debug APK。

## 6. 当前完成度判断

当前状态可以认为：

- MVP 主流程已经可运行。
- Android 工程已经具备继续迭代的基础。
- 首页、阅读、扫描、导出、设置五个主模块已经成型。
- Reader 最新需求已完成到模拟器验证。
- 单元测试通过。
- Debug APK 可安装运行。

当前不建议直接视为正式上架版本，因为还缺少发布前外部工作。

## 7. 仍需处理的事项

### 发布前必须补充

- 真机测试，尤其是相机、文件选择器、分享面板。
- 正式签名密钥。
- App 图标、启动图、应用名称最终确认。
- 隐私政策和权限说明。
- Play Store / 应用商店发布资料。
- Pro / 订阅 / 付费逻辑的真实后端或 Play Billing 接入。

### 产品体验继续优化

- Reader 顶部工具区可以继续做成更沉浸式的 PDF 阅读器样式。
- 文件管理可以扩展为文件夹、收藏、最近打开、批量选择。
- PDF 编辑能力可以继续拆分：
  - 真实文本标注
  - 手写批注
  - 页面重排
  - OCR
  - 批量压缩
  - 批量转换

### 测试继续补充

- Reader 精简后的完整 UI 回归。
- 最新版本像素级验收全集。
- 多 PDF 压力测试。
- 大文件测试。
- 横屏和平板布局测试。
- 真机相机测试。

## 8. 建议下一步

建议下一步先做 Reader 最新版本的完整 UI 回归和像素验收，把这次阅读页精简后的结果正式纳入验收报告。

之后再进入下一轮产品功能调整，例如：

- 首页工具入口是否需要删减或重排。
- Reader 工具栏是否需要隐藏底部导航，做成沉浸阅读模式。
- Sign / Note / Bookmark 是否要真正进入独立编辑流程。
- 文件列表是否增加 Rename / Delete / Share / Favorite。

