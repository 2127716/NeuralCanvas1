# 🧠 NeuralCanvas

**一款面向知识整理、思维建模与关系可视化的 Android 思维地图应用。**

[![Android CI](https://github.com/2127716/NeuralCanvas1/actions/workflows/android.yml/badge.svg)](https://github.com/2127716/NeuralCanvas1/actions/workflows/android.yml)
[![Android](https://img.shields.io/badge/Android-21%2B-brightgreen?style=flat-square&logo=android)](https://www.android.com)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)

---

## 📱 项目简介

NeuralCanvas 是一个 Android 端思维地图 / 关系图谱应用，用于：

- 梳理知识结构
- 连接想法与任务
- 可视化问题、资源、目标之间的关系
- 在画布中自由组织节点网络

当前版本支持多类型节点、方向连线、搜索、自动保存、节点预览、连线编辑等功能；并正在扩展 **AI 图谱理解、AI 辅助建图、知识导入自动整理** 等能力。

---

## ✨ 当前功能

### 基础图谱编辑
- 新建节点
- 编辑节点标题、内容、类型、形状
- 长按节点打开编辑弹窗
- 双击空白区域快速创建节点
- 拖拽移动节点
- 清空全部内容

### 节点系统
支持以下节点类型：
- 概念（CONCEPT）
- 想法（IDEA）
- 问题（QUESTION）
- 资源（RESOURCE）
- 任务（TASK）
- 目标（GOAL）
- 笔记（NOTE）
- 决策（DECISION）

支持以下节点形状：
- 正方形
- 圆形
- 椭圆
- 菱形
- 三角形
- 五边形
- 六边形

### 连线系统
- 支持节点间有方向连线
- 支持连线标签
- 支持连线颜色与粗细
- 支持连线选中
- 支持箭头方向可视化

### 浏览与交互
- 双指缩放
- 拖动画布
- 节点点击预览
- 搜索节点
- 高亮搜索结果
- 聚焦匹配节点

### 数据能力
- 自动保存当前图谱
- 启动时自动恢复上次内容
- 使用 SharedPreferences + Gson 进行本地序列化存储

---

## 🤖 AI 功能（开发中 / 接入中）

项目正在接入 AI 图谱辅助系统，目标能力包括：

### AI 读取整张图谱
AI 能读取：
- 所有节点标题
- 节点内容
- 节点类型
- 节点形状
- 节点位置
- 节点之间的连接关系
- 连线方向
- 连线标签

### AI 问答
例如：
- “总结当前图谱的核心主题”
- “找出图中可能冲突的节点”
- “哪些任务节点缺少前置资源”
- “这个结构里有哪些逻辑断层”

### AI 编辑图谱
例如：
- 自动补充子节点
- 自动建立连线
- 修改节点内容
- 调整连线类型
- 自动布局

### 知识导入
可将一段文本交给 AI 自动整理为：
- 核心主题节点
- 子概念节点
- 因果 / 依赖 / 参考关系
- 初始知识网络结构

### 命令预览机制
AI 不直接修改画布，而是先生成结构化命令：
- create_node
- update_node
- delete_node
- create_connection
- update_connection
- delete_connection
- focus_node
- auto_layout

用户确认后再执行，避免误改。

> 注意：AI 功能需要自行配置兼容 OpenAI Chat Completions 风格的接口地址、API Key 和模型名。

---

## 🛠️ 技术栈

- **语言**：Java
- **平台**：Android
- **最低支持**：Android 5.0（API 21）
- **编译 SDK**：33
- **目标 SDK**：33
- **构建工具**：Gradle
- **核心依赖**：
  - AndroidX AppCompat 1.6.1
  - Material Components 1.9.0
  - ConstraintLayout 2.1.4
  - MultiDex 2.0.1
  - Gson 2.10.1
  - OkHttp 4.12.0（AI 接口请求）

---

## 📁 当前项目结构

```text
NeuralCanvas1/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/agui/neuralcanvas/
│       │   ├── MainActivity.java
│       │   ├── MindMapView.java
│       │   ├── Node.java
│       │   ├── Connection.java
│       │   ├── NodeEditDialog.java
│       │   ├── SearchDialog.java
│       │   ├── HelpActivity.java
│       │   ├── SimpleDataManager.java
│       │   ├── AiConfig.java
│       │   ├── AiGraphSnapshot.java
│       │   ├── AiCommand.java
│       │   ├── AiResponse.java
│       │   ├── AiJsonParser.java
│       │   ├── AiRepository.java
│       │   ├── AiGraphExecutor.java
│       │   ├── AiAssistantDialog.java
│       │   ├── KnowledgeImportDialog.java
│       │   ├── AiCommandPreviewDialog.java
│       │   └── GraphAutoLayout.java
│       └── res/
│           ├── layout/
│           ├── menu/
│           ├── values/
│           └── drawable/
├── .github/
│   └── workflows/
│       └── android.yml
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle
└── README.md

---

🚀 本地构建
Windows
Bash
gradlew.bat assembleDebug
macOS / Linux
Bash
./gradlew assembleDebug
构建完成后，APK 一般位于：
Plain text
app/build/outputs/apk/debug/app-debug.apk
☁️ GitHub Actions 自动构建
仓库已配置 GitHub Actions 自动构建工作流：android.yml
触发方式：
push 到 main 或 master
Pull Request 到 main 或 master
手动点击 Run workflow
构建完成后可在 Actions 页面下载：
Debug APK
构建报告（如果有）
工作流地址：
https://github.com/2127716/NeuralCanvas1/actions/workflows/android.yml⁠�
🔐 AI 接口配置说明
在应用内 AI 助手中需要填写：
Base URL：例如某兼容 OpenAI 格式接口的 /v1
API Key
Model：例如 deepseek-chat 等
应用会自动将：
当前节点
当前连线
结构关系
整理为 JSON 发给 AI，再由 AI 返回结构化命令进行预览与执行。
📌 项目现状说明
当前项目重点在于：
优化图谱编辑体验
保持本地保存稳定
接入 AI 图谱助手
实现知识文本自动建图
增强布局与可视化能力
部分 AI 功能仍在持续完善中，README 会随着功能落地持续更新。
🤝 反馈与建议
如果你在使用或修改项目时遇到问题，可以通过以下方式反馈：
提交 Issue
提交 Pull Request
在代码基础上自行扩展本地功能
📄 说明
本项目当前更偏个人实验性与功能迭代型开发，README 以“当前已实现 + 正在接入的方向”为准。
---

## 第三步：提交 README 修改

1. 往下滑到页面最底部
2. 在提交说明里填：`update README`
3. 选默认的 **Commit directly to the main branch**
4. 点 **Commit changes**

---

## 第四步：刷新仓库首页检查效果

你提交后回到仓库首页，重点看这几个地方：

1. 顶部标题下面是不是出现了新的项目简介
2. 徽章是不是正常显示
3. “当前功能”“AI 功能”“项目结构”这些标题是不是都排版正常
4. 代码块里的项目结构是不是等宽字体显示
5. `android.yml` 链接点进去能不能打开

---

## 第五步：你现在项目界面还该继续完善的地方

README 改完后，GitHub 仓库页面通常还要继续整理这几个位置：

- 仓库右侧 **About**
- Topics 标签
- Releases
- Actions 页面说明
- 项目封面图 / 截图

但你现在先别一起搞乱。  
先把 README 成功改好。

---

## 你下一步该做什么

你先把上面这份 README **完整替换并提交**。  
提交完以后，把你仓库首页截图发我，或者直接跟我说“README 改好了”，我下一步再**一步一步教你改 GitHub 右侧 About 和 Topics**，让项目界面更像一个完整项目。


