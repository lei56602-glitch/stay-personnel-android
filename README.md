# 住宿人员名单统计系统 Android

这是完整原生 Android 实现，不使用 WebView、HTML、CSS 或 JavaScript 作为应用界面或业务运行环境。

## 原生实现

- Android 原生 View 构建界面，保留原页面的蓝紫渐变按钮、浅色渐变背景、玻璃质感卡片、输入区域、搜索标签、统计信息、横向数据表格和底部悬浮添加按钮的布局方向。
- Java 原生实现智能识别、单人/批量录入、同宿舍合并、搜索与高级筛选、编辑、删除和统计。
- 使用 SharedPreferences 在本机持久化 JSON 记录，记录字段沿用 `id`、`stayType`、`roomNum`、`personList`。
- 使用 Android 系统文件保存窗口导出 XLSX，不依赖 CDN 或外部网络。
- 不含广告 SDK，不申请互联网或存储权限。

## 构建

GitHub Actions 使用 JDK 17、Gradle 8.9 和 Android Gradle Plugin 构建 Debug APK。

```bash
gradle assembleDebug
```

APK：`app/build/outputs/apk/debug/app-debug.apk`
