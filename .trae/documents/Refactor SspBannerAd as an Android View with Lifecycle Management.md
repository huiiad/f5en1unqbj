# SSP SDK & Demo App Implementation Plan

This plan outlines the creation of an Android Advertising SDK and a Demo Application to test it. The SDK will support Banner, Native, and Video ads with comprehensive tracking and state management.

## 1. Project Structure Setup

* **Create Module Directories**: Initialize `ssp_sdk` and `demo_app` directories.

* **Configure Build Files**:

  * **`ssp_sdk/build.gradle`**: Android Library configuration, `minSdk` 21, `compileSdk` 34.

  * **`demo_app/build.gradle`**: Android Application configuration, depends on `:ssp_sdk`.

* **Manifests**: Define permissions (Internet, Network State) and application components.

## 2. SDK Core Implementation (`ssp_sdk`)

### Initialization & Configuration

* **`SspSdk`** **Class**: Singleton entry point.

  * `init(Context context, String appId)`: Initializes the SDK and pre-fetches device information.

* **`DeviceInfoManager`**: Helper class to gather:

  * Screen resolution (`deviceW`, `deviceH`).

  * User Agent (`ua`).

  * OS Version, Model, Brand.

  * Advertising ID (`ifa`) - *Note: Will use a placeholder/mock implementation to avoid heavy Play Services dependencies for this demo.*

### Networking & Data Models

* **`AdRequest`**: POJO/Builder to construct the JSON request body (matches user's example: `os`, `ifa`, `deviceH`, etc.).

* **`AdResponse`**: JSON parser to handle the SSP response (supports `VIDEO` structure provided, plus `BANNER`/`NATIVE`).

* **`NetworkClient`**: A lightweight `HttpURLConnection` wrapper to send POST requests and handle responses on a background thread.

### Ad Units & Logic

* **`SspAdListener`**: Interface for callbacks:

  * `onAdLoaded()`, `onAdFailed(String error)`

  * `onImpression()`, `onAdClicked()`

* **`BaseAd`**: Abstract class managing `loadAd`, `AdListener`, and common tracking logic (`impTrackers`, `clickTrackers`).

* **`SspBannerAd`**:

  * Loads HTML content into a `WebView`.

  * Handles simple impression tracking.

* **`SspNativeAd`**:

  * Parses native assets (Title, Icon, Description, CTA).

  * Provides a method `registerViewForInteraction(View view)` to handle clicks.

* **`SspVideoAd`**:

  * **Rendering**: Uses `VideoView` for playback.

  * **Tracking**: Monitors playback progress to fire VAST events (`start`, `firstQuartile`, `midpoint`, `thirdQuartile`, `complete`).

  * **Click Handling**: Handles clicks to open the landing page.

## 3. Demo App Implementation (`demo_app`)

* **`MainActivity`**: The test harness.

  * **Init Section**: Button to initialize SDK.

  * **Banner Section**: Container to display the banner.

  * **Native Section**: Layout to render native ad elements (Title, Image, Button).

  * **Video Section**: Area to play the video ad.

* **Logs**: On-screen log console to visualize SDK callbacks (`onLoaded`, `onImpression`, etc.).

## 4. Verification

* **Build Check**: Ensure both modules compile with Android SDK 34.

* **Functional Test**:

  * Verify `init` collects device info.

  * Verify `loadAd` sends correct JSON parameters.

  * Verify Video Ad plays and fires tracking events (simulated via logs).

## Key Technical Decisions

* **Language**: Java (as requested).

* **Target SDK**: Android 34.

* **Dependencies**: Minimal external dependencies to keep the SDK lightweight. JSON parsing will use `org.json` (Android standard).

