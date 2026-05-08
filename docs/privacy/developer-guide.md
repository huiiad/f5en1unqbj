# SSP SDK 开发者隐私合规指南

## 概述

本指南旨在帮助开发者正确集成 SSP SDK，并确保符合隐私保护法规的要求。本指南作为 [SSP SDK 隐私权政策](privacy-policy) 的补充，提供具体的实施建议和最佳实践。

## 隐私合规清单

### 必检项

- [ ] 在应用隐私政策中披露 SSP SDK 数据收集
- [ ] 实施最终用户同意机制
- [ ] 提供用户退出广告追踪的选项
- [ ] 未满 13 岁儿童应用需额外处理

### 推荐项

- [ ] 在权限请求时说明用途
- [ ] 提供应用内隐私设置
- [ ] 记录用户同意状态

## 隐私政策披露模板

### 基本披露文本

建议在您的隐私政策中添加以下内容：

```markdown
## 广告服务

我们的应用集成了第三方广告 SDK（SSP SDK），用于在应用中展示广告。在提供广告服务的过程中，SSP SDK 可能会收集以下信息：

- **设备信息**：设备品牌、型号、屏幕尺寸、操作系统版本
- **广告标识符**：Google 广告 ID (GAID)
- **网络信息**：IP 地址、连接类型
- **地理位置**：经纬度坐标（需用户授权）
- **广告交互数据**：广告展示、点击等事件

这些信息用于：
- 提供个性化广告
- 广告效果统计
- 防止无效广告流量

您可以通过以下方式限制广告追踪：
- 设备设置 > Google > 广告 > 退出广告个性化
- 或联系：privacy@ssp-sdk.com

详情请参阅：[SSP SDK 隐私权政策](https://ssp-sdk.github.io/ssp-sdk-android/privacy-policy)
```

### 儿童应用附加要求

如果您的应用面向 13 岁以下儿童，请添加：

```markdown
## 儿童隐私

我们不会故意收集 13 岁以下儿童的个人信息。如果您的应用面向儿童，请确保：
1. 获得监护人的明确同意
2. 遵守 COPPA（儿童在线隐私保护法）等相关法规
3. 限制定向广告的使用
```

## 同意流程设计

### 推荐的用户流程

1. **首次启动**
   - 显示隐私说明对话框
   - 解释数据收集用途
   - 提供「同意」和「不同意」选项

2. **权限请求**
   - 在请求位置权限前说明用途
   - 示例：`"此权限用于向您展示本地化广告，可提升广告相关性"`

3. **设置页面**
   - 提供广告个性化开关
   - 链接到系统广告设置

### 代码示例

```java
public class PrivacyHelper {
    
    // 检查是否需要显示同意对话框
    public static boolean shouldShowConsentDialog(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("privacy", MODE_PRIVATE);
        return prefs.getBoolean("consent_given", false);
    }
    
    // 记录用户同意
    public static void recordConsent(Context context, boolean accepted) {
        SharedPreferences prefs = context.getSharedPreferences("privacy", MODE_PRIVATE);
        prefs.edit()
            .putBoolean("consent_given", true)
            .putBoolean("consent_accepted", accepted)
            .putLong("consent_timestamp", System.currentTimeMillis())
            .apply();
    }
    
    // 引导用户到系统广告设置
    public static void openAdSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_AD_SETTINGS);
            context.startActivity(intent);
        } catch (Exception e) {
            // 部分设备可能不支持此设置页面
        }
    }
}
```

## 权限使用说明

### 必须声明的权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 可选权限（用于增强广告定向）

```xml
<!-- 精确定位 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- 粗略定位 -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- WiFi 状态（用于判断网络类型）-->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

### 动态权限请求

```java
// 仅在需要时请求位置权限
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
    != PackageManager.PERMISSION_GRANTED) {
    
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        REQUEST_LOCATION_PERMISSION);
}

// 权限回调
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_LOCATION_PERMISSION) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予
        } else {
            // 权限被拒绝
        }
    }
}
```

## 数据安全最佳实践

### 传输安全

- 确保所有 API 调用使用 HTTPS
- 实现证书锁定（Certificate Pinning）
- 验证服务器证书

### 本地存储

- 敏感数据不要明文存储
- 使用 EncryptedSharedPreferences
- 定期清理不需要的数据

## 常见问题

### Q: SSP SDK 会收集哪些敏感信息？

**A:** SSP SDK 会收集：
- 广告标识符（GAID）
- IP 地址
- 设备信息（品牌、型号、屏幕尺寸）
- 位置信息（需用户授权）

### Q: 如何帮助用户退出广告追踪？

**A:** 引导用户前往系统设置：
- 设置 > Google > 广告 > 退出广告个性化

或使用代码打开系统广告设置页面：

```java
Intent intent = new Intent(Settings.ACTION_AD_SETTINGS);
startActivity(intent);
```

### Q: 是否需要向 SSP 提供用户同意证明？

**A:** 不需要，但我们建议开发者自行保留同意记录，以备监管机构查询。

### Q: SDK 初始化时需要用户同意吗？

**A:** 我们建议在 SDK 初始化前获取用户同意。您可以：

1. 显示隐私说明对话框
2. 用户同意后再调用 `SspSdk.init()`
3. 记录用户的同意状态

### Q: 如果用户拒绝同意怎么办？

**A:** 您可以选择：
1. 不展示广告
2. 展示不基于用户数据的通用广告
3. 记录拒绝状态，但不阻止用户使用应用

## 联系我厜

- 技术支持：support@ssp-sdk.com
- 隐私咨询：privacy@ssp-sdk.com
- 数据保护官：dpo@ssp-sdk.com

## 更新日志

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2024-01-01 | 1.0 | 初始版本 |
