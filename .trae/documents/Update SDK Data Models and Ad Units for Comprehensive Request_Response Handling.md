# Implement Full Ad Request & Response Logic

I will update the SDK to handle the detailed Request/Response structures provided for Banner, Native, and Video ads.

## 1. Update Data Models

### `AdRequest`
*   Add fields to match the provided JSON:
    *   `sourceType` (default "SDK")
    *   `osv` (OS Version)
    *   `model`, `brand`, `appver`
    *   `deviceW`, `deviceH` (ensure they match `width`/`height` or screen size)
    *   `connectiontype` (int)
*   Ensure `adType` uses the Enum name (already done, but verify).

### `AdResponse`
*   Refactor to a comprehensive model that abstracts common parts and includes type-specific fields.
*   **Common Fields**: `id`, `impId`, `adType`, `price`, `currency`, `width`, `height`, `creativeId`, `adomain`, `billingType`, `impTrackers`, `clickTrackers`, `clickUrl`, `dealId`, `ext`.
*   **Banner Specific**: `htmlContent` (mapped from `htmlContent` in JSON, or `adm` if needed).
*   **Native Specific**: `title`, `body` (or `description`), `cta` (or `callToAction`), `iconUrl`, `imageUrl`, `eventTrackers` (map to internal tracking structure).
*   **Video Specific**:
    *   `duration`, `rewarded`, `renderWidth`, `renderHeight`, `skippable`.
    *   `mediaFiles` (List of objects with `url`, `type`, `bitrate`, etc.).
    *   `trackingEvents` (Map for VAST events: `start`, `firstQuartile`, etc.).

## 2. Update Ad Units

### `SspVideoAd`
*   Update parsing to extract `mediaFiles` (use the first valid MP4).
*   Update event tracking to use the new `trackingEvents` map from the response.
*   Implement full VAST event firing (`start`, `midpoint`, `complete`, etc.) based on the new data structure.

### `SspNativeAd`
*   Update getters (`getTitle`, `getBody`, etc.) to map from the new `AdResponse` fields.
*   Ensure `registerViewForInteraction` handles the new tracking logic if needed.

### `SspBannerAd`
*   Ensure `htmlContent` is correctly extracted and rendered.

## 3. Implementation Steps

1.  **Modify `AdRequest.java`**: Add missing fields and populate them from `DeviceInfoManager`.
2.  **Modify `AdResponse.java`**: Add all fields from the provided examples and update the `parse` method to handle the specific JSON structures (including nested arrays/objects like `mediaFiles` and `trackingEvents`).
3.  **Update `SspVideoAd.java`**: Bind the video player to the new `mediaFiles` URL and wire up VAST events.
4.  **Update `SspNativeAd.java`**: Align getters with new response fields.
5.  **Verify `SspBannerAd.java`**: Ensure it uses the correct HTML content field.

This plan ensures the SDK can handle the exact JSON formats provided for all three ad types.