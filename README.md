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
