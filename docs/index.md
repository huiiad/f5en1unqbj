# SSP SDK Android 开发者文档

欢迎使用 SSP SDK！本 SDK 为 Android 应用开发者提供全面的广告变现解决方案。

## 📚 文档目录

### 隐私合规

- [隐私权政策](./privacy/index.md) - SSP SDK 官方隐私政策
- [开发者隐私合规指南](./privacy/developer-guide.md) - 开发者集成指南

### API 文档

- [SDK 初始化](./api/init.md) - 如何初始化 SDK
- [广告位配置](./api/ads.md) - 配置广告位
- [事件监听](./api/listeners.md) - 监听广告事件

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'com.ssp.sdk:ssp-sdk:1.0.0'
}
```

### 2. 配置 AndroidManifest

```xml
<meta-data
    android:name="com.ssp.sdk.APP_ID"
    android:value="YOUR_APP_ID" />
```

### 3. 初始化 SDK

```java
// 在 Application 类中初始化
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SspSdk.getInstance().init(this);
    }
}
```

## 📊 支持的广告类型

| 广告类型 | 说明 | 尺寸 |
|---------|------|------|
| Banner | 横幅广告 | 320x50, 320x100, 300x250 |
| Native | 原生广告 | 自适应 |
| Video | 视频广告 | 全屏 |

## 🔒 隐私合规

集成 SSP SDK 前，请确保：

1. 阅读 [隐私权政策](./privacy/index.md)
2. 遵循 [开发者隐私合规指南](./privacy/developer-guide.md)
3. 在应用隐私政策中披露数据收集

## 📞 技术支持

- 邮箱：support@ssp-sdk.com
- 官网：https://ssp-sdk.com

## 📄 许可证

Copyright © 2024 SSP SDK. All rights reserved.
