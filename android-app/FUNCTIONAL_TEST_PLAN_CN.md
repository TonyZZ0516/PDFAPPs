# SwiftPDF 功能完整性测试清单

更新时间：2026-05-22

## 当前自动验证

- Debug APK 构建：通过。
- 模拟器安装 APK：通过。
- 冷启动 MainActivity：通过。
- 启动后无 AndroidRuntime / FATAL EXCEPTION 日志：通过。
- Export / Share 模块构建：通过。
- Settings 模块构建：通过。

## MVP 主流程测试

### 1. Home / 文件导入

- 打开 App 后进入 Home。
- 点击 `Choose PDF` 打开系统文件选择器。
- 选择 PDF 后自动进入 Reader。
- Home 最近文件出现该 PDF。
- 重启 App 后最近文件仍然存在。
- 点击最近文件进入 Reader。
- 点击删除按钮后，仅从最近文件移除，不删除原始文件。

### 2. Reader / PDF 阅读

- Reader 显示当前 PDF 文件名和文件大小。
- 第一页渲染成功。
- `Page X of Y` 正确显示。
- `Previous` 在第一页禁用。
- `Next` 可进入下一页。
- 最后一页 `Next` 禁用。
- 损坏或无法读取的 PDF 显示错误状态，不崩溃。

### 3. Scan / 扫描生成 PDF

- 进入 Scan 页面显示相机权限说明。
- 点击 `Enable Camera` 弹出系统相机权限。
- 授权后显示 CameraX 相机预览。
- 点击 `Capture Page` 保存一张扫描图片。
- 已拍页面列表数量增加。
- 可删除已拍页面。
- 无已拍页面时 `Export Scan to PDF` 禁用。
- 有已拍页面时可导出 PDF。
- 导出成功后自动进入 Reader。
- 导出的 PDF 出现在 Home 最近文件。

## 后续模块测试

### Export / 分享

- 当前 PDF 显示文件名、大小、操作按钮。
- 分享 Intent 可唤起系统分享面板。
- App 内部生成的 PDF 通过 FileProvider 安全分享。
- content URI 导入的 PDF 通过系统授权 URI 分享。
- 打开文件可交给系统 PDF 阅读器或回到内置 Reader。

### OCR

- OCR 入口可选择当前 PDF 或扫描页。
- 识别中显示进度。
- 成功后展示文本结果。
- 失败时展示错误状态。

### Pro / 限制

- 免费限制触发时显示 Pro Gate。
- 取消后回到当前任务。
- 已订阅状态绕过限制。

### Settings / 设置

- 显示本地最近文件数量。
- 可清空最近文件历史。
- 清空历史不删除用户原始文件。
- 显示存储说明、默认导出质量、版本信息。

## 当前未完成项

- 扫描页缩略图真实预览。
- 扫描页重拍。
- 扫描图像裁切、旋转、增强。
- 导出结果页。
- OCR。
- 压缩、合并、拆分。
- Reader 缩放、搜索、缩略图。
- Pro 订阅。
