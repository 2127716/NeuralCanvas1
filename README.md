# 🧠 NeuralCanvas

**专为INTJ思维模式设计的思维地图工具**

[![GitHub Actions Build](https://img.shields.io/github/actions/workflow/status/YOUR_USERNAME/NeuralCanvas/build.yml?style=flat-square)](https://github.com/YOUR_USERNAME/NeuralCanvas/actions)
[![Android](https://img.shields.io/badge/Android-21%2B-brightgreen?style=flat-square&logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

## 📱 应用简介

NeuralCanvas是一个专为INTJ思维模式设计的思维地图工具，帮助用户可视化复杂的思维结构、连接想法、组织知识体系。

## ✨ 核心功能

- **节点类型系统**: 8种思维节点（概念、想法、问题、资源等）
- **可视化连接**: 拖拽式节点连接，直观展示思维关系
- **层级结构**: 无限层级的思维地图组织
- **JSON导入导出**: 完整的数据持久化和分享功能
- **Material Design**: 现代化的用户界面设计

## 🚀 快速开始

### 在线构建（推荐）
项目已配置GitHub Actions，支持云端自动构建：

1. **上传到GitHub**: 将项目上传到GitHub仓库
2. **触发构建**: 推送代码或手动触发GitHub Actions
3. **下载APK**: 从构建产物中下载生成的APK文件

详细指南请查看 [GITHUB_ACTIONS_GUIDE.md](GITHUB_ACTIONS_GUIDE.md)

### 本地构建
```bash
# 使用Gradle Wrapper构建
./gradlew assembleDebug    # 构建调试版
./gradlew assembleRelease  # 构建发布版
```

## 🛠️ 技术栈

- **语言**: Java
- **框架**: Android SDK 33
- **构建工具**: Gradle 7.4.2 + AGP 7.4.2
- **依赖**:
  - AndroidX AppCompat 1.6.1
  - Material Design 1.9.0
  - ConstraintLayout 2.1.4
  - Gson 2.10.1 (JSON序列化)

## 📁 项目结构

```
NeuralCanvas/
├── app/
│   ├── src/main/java/com/agui/neuralcanvas/
│   │   ├── MainActivity.java          # 主界面
│   │   ├── CanvasView.java            # 画布视图
│   │   ├── Node.java                  # 节点数据模型
│   │   ├── Connection.java            # 连接数据模型
│   │   ├── NodeType.java              # 节点类型枚举
│   │   ├── DataManager.java           # 数据管理
│   │   └── NodeDialog.java            # 节点编辑对话框
│   ├── src/main/res/                  # 资源文件
│   └── build.gradle                   # 应用构建配置
├── .github/workflows/build.yml        # GitHub Actions配置
├── build.gradle                       # 项目构建配置
├── gradle.properties                  # Gradle属性
├── gradlew                            # Gradle Wrapper脚本
└── settings.gradle                    # 项目设置
```

## 🔧 开发环境

- **最小SDK**: Android 5.0 (API 21)
- **目标SDK**: Android 13 (API 33)
- **编译SDK**: Android 13 (API 33)
- **Java版本**: 1.8

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📞 联系

如有问题或建议，请通过GitHub Issues联系。

---

**构建状态**: [![GitHub Actions](https://github.com/YOUR_USERNAME/NeuralCanvas/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/NeuralCanvas/actions)

*最后更新: $(date)*