# SSP SDK 接入指南

本指南将指导您如何将 SSP SDK 集成到您的 Android 应用中，以展示横幅广告 (Banner)、原生广告 (Native) 和视频广告 (Video)。

## 1. 安装

### 添加依赖
请确保已将 SDK 模块或 AAR 添加到您的项目中。

## 2. 初始化

在加载任何广告之前，必须先初始化 SDK。建议在 `Application` 类或主 `Activity` 中完成此操作。

### 方法 A: Manifest 配置（推荐）
在您的 `AndroidManifest.xml` 中添加 App ID：

```xml
<meta-data 
    android:name="com.ssp.sdk.APP_ID" 
    android:value="YOUR_APP_ID_HERE" />
```

然后调用 init 方法：
```java
SspSdk.getInstance().init(context);
```

### 方法 B: 代码动态初始化
```java
SspSdk.getInstance().init(context, "YOUR_APP_ID_HERE");
```

## 3. 广告类型

### 3.1 横幅广告 (Banner Ad)
横幅广告是占据应用布局中一部分位置的矩形图片或文字广告。

**布局 (XML):**
```xml
<FrameLayout
    android:id="@+id/banner_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**代码示例:**
```java
// 1. 创建广告实例
SspBannerAd bannerAd = new SspBannerAd(this, "YOUR_SLOT_ID");

// 2. 添加到视图层级
FrameLayout container = findViewById(R.id.banner_container);
container.addView(bannerAd);

// 3. 设置监听器
bannerAd.setLoadListener(new SspLoadListener() {
    @Override
    public void onAdLoaded(BaseAd ad) {
        // 广告加载成功
    }
    @Override
    public void onAdFailed(String error) {
        // 广告加载失败
    }
});

bannerAd.setInteractionListener(new SspInteractionListener() {
    @Override
    public void onImpression() {
        // 广告展示
    }
    @Override
    public void onAdClicked() {
        // 广告点击
    }
    @Override
    public void onAdShowFailed(String error) {
        // 展示失败
    }
});

// 4. 加载并展示
// 方式 A: 自动加载并展示 (推荐)
// 此方法会自动处理自动刷新、失败重试等逻辑
bannerAd.loadAndShow();

// 方式 B: 手动控制
// bannerAd.loadAd();
// 在 onAdLoaded 回调中调用 bannerAd.showAd();

// 5. 生命周期管理
// SDK 会自动绑定到 Activity/Fragment 的生命周期 (Pause/Resume/Destroy) 以管理资源。
// 只要您的 Context 是 Activity 或实现了 LifecycleOwner，无需手动调用生命周期方法。

/* 仅在特殊场景（如非标准 Activity 环境）下需要手动调用：
@Override
protected void onPause() {
    super.onPause();
    if (bannerAd != null) {
        bannerAd.pause();
    }
}

@Override
protected void onResume() {
    super.onResume();
    if (bannerAd != null) {
        bannerAd.resume();
    }
}

@Override
protected void onDestroy() {
    if (bannerAd != null) {
        bannerAd.destroy();
    }
    super.onDestroy();
}
*/
```

### 3.2 原生广告 (Native Ad)
原生广告允许您自定义广告的外观，使其与应用的整体设计风格保持一致。

**代码示例:**
```java
SspNativeAd nativeAd = new SspNativeAd(this, "YOUR_SLOT_ID");

nativeAd.setLoadListener(new SspLoadListener() {
    @Override
    public void onAdLoaded(BaseAd ad) {
        // 使用返回的 ad 对象（或 nativeAd 实例）进行渲染
        if (ad instanceof SspNativeAd) {
             renderNativeAd((SspNativeAd) ad);
        }
    }
    // ... 处理失败情况
});

nativeAd.loadAd();

// 渲染辅助方法
private void renderNativeAd(SspNativeAd ad) {
    String title = ad.getTitle();
    String desc = ad.getDescription();
    String iconUrl = ad.getIconUrl();
    String imageUrl = ad.getImageUrl();
    
    // 将这些数据绑定到您的自定义 View (TextView, ImageView)
    myTitleView.setText(title);
    
    // 重要：注册视图以处理交互 (点击/曝光)
    // 必须提供容器视图和可点击的视图 (例如 CTA 按钮)
    ad.registerViewForInteraction(myNativeAdContainer, myCtaButton);
}
```

### 3.3 视频广告 (Video Ad)
视频广告通常为全屏展示的广告形式。

**代码示例:**
```java
SspVideoAd videoAd = new SspVideoAd(this, "YOUR_SLOT_ID");

videoAd.setLoadListener(new SspLoadListener() {
    @Override
    public void onAdLoaded(BaseAd ad) {
        // 视频准备就绪，可以展示
        if (videoAd.isValid()) {
            videoAd.showAd();
        }
    }
    // ... 处理失败情况
});

videoAd.loadAd();
```

## 4. 回调与监听器

SDK 提供两个主要的监听接口：

*   **`SspLoadListener`**: 处理加载生命周期 (`onAdLoaded`, `onAdFailed`)。
*   **`SspInteractionListener`**: 处理用户交互 (`onImpression`, `onAdClicked`, `onAdShowFailed`)。

## 5. 统计与上报
SDK 会自动处理以下数据的上报，无需开发者手动干预：
*   SDK 初始化
*   广告请求 (成功/失败)
*   广告展示 (Impression)
*   广告点击 (Click)

## 6. 混淆规则 (ProGuard)
如果您使用了 ProGuard 或 R8 进行代码混淆，请添加以下规则：

```proguard
-keep class com.ssp.sdk.** { *; }
-keep interface com.ssp.sdk.** { *; }
```

## 7. Jetpack Compose 接入

SDK 支持在 Jetpack Compose 中使用，可以通过 `AndroidView` 组合项进行桥接。

### 接入示例 (Banner)

```kotlin
@Composable
fun SspBannerAdCompose(
    slotId: String = "slot_banner_001",
    modifier: Modifier = Modifier
) {
    // 1. 获取 Compose 环境下的 Context
    val context = LocalContext.current
    
    // 2. 使用 AndroidView 桥接
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            // factory 只在 View 首次创建时执行一次
            // 在这里初始化 View 并开始加载，无需额外的 state 标志位
            SspBannerAd(ctx, slotId).apply {
                // 设置监听器 (可选)
                setLoadListener(object : SspLoadListener {
                    override fun onAdLoaded(ad: BaseAd?) {
                        Log.d("SspAd", "onAdLoaded: $ad")
                    }
                    override fun onAdFailed(error: String?) {
                        Log.d("SspAd", "onAdFailed: $error")
                    }
                })
                
                // 加载并展示
                loadAndShow()
            }
        },
        // update 在 View 更新时调用 (例如 modifier 变化)，此处无需操作
        update = { view -> 
            // 如果需要根据 Compose state 更新广告配置，写在这里
        },
        // onRelease 当组件销毁时调用，用于资源释放 (Compose 1.3.0+)
        onRelease = { view ->
            view.destroy()
        }
    )
}
```

对于 **原生广告 (Native Ad)**，您可以使用 `AndroidView` 包裹一个自定义的 `FrameLayout` 或 `LinearLayout`，并在 `update` 块中调用 `renderNativeAd` 逻辑和 `registerViewForInteraction`。

对于 **视频广告 (Video Ad)**，建议在 `LaunchedEffect` 中加载，并使用 `LocalContext.current` 获取 Activity 上下文来调用 `showAd()`。
