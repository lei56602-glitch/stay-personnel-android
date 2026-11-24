# 住宿人员名单统计系统 Android

将原始单页 HTML 应用封装为 Android WebView APK。

- 保留原有住宿人员录入、智能识别、搜索筛选、编辑删除、统计与本地数据逻辑。
- WebView 开启 DOM Storage，沿用原有 localStorage 键 `stayPersonData`。
- Android 端 Excel 导出由原生代码生成 Office Open XML 工作簿，通过系统“创建文档”窗口保存，不依赖网络。
- 不包含广告 SDK，不申请不必要的存储权限。
- GitHub Actions 在推送到 main 后自动构建 debug APK。

## 构建

在安装 Android SDK、JDK 17 和 Gradle 8.9 的环境运行：

```bash
gradle assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
