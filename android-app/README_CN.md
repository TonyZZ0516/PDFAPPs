# SwiftPDF Android 工程

这是 SwiftPDF 的 Android 原生工程骨架，当前目标是先跑通 Compose App 壳、导航结构和页面占位，再逐步接入 PDF 阅读、扫描、OCR、导出和压缩能力。

## 当前结构

```text
android-app/
  app/
    src/main/
      AndroidManifest.xml
      java/com/swiftpdf/app/
        MainActivity.kt
        navigation/
        ui/
          components/
          screens/
          theme/
```

## 开发阶段

1. Phase 0：工程骨架、主题、导航、页面占位。
2. Phase 1：Home 文档列表、最近文件、导入入口。
3. Phase 2：PDF Reader 阅读器、缩略图、页码、搜索入口。
4. Phase 3：Camera Scan 扫描流程、边缘裁切占位、保存为 PDF。
5. Phase 4：OCR、导出、压缩、Pro 触发点。

## 本地运行

用 Android Studio 打开 `android-app/`，等待 Gradle Sync 完成后运行 `app` 模块。

当前仓库还没有提交 Gradle Wrapper。如果需要命令行构建，后续可以在 Android Studio 或本机 Gradle 环境中生成 wrapper。
