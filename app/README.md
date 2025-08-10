# Samsung APK Downloader - Android App

## Project Overview

This Android application is a direct port of the Python script `SamsungApkDownloader.py`. It allows users to download Samsung APK files directly from Samsung servers using device model, SDK version, and package name.

## Current Status: Phase 1 - Super Basic Version ✅

The app implements the core functionality with minimal UI as specified in the development plan:

### Features Implemented
- ✅ Three plain text input fields (Device Model, SDK Version, Package Name)
- ✅ Core Samsung API integration
- ✅ HTTP GET requests to Samsung servers
- ✅ XML response parsing using regex (same pattern as Python script)
- ✅ Version code and version name display
- ✅ Y/N download confirmation
- ✅ APK download to Downloads folder using DownloadManager
- ✅ Basic completion messages and error handling
- ✅ Storage permissions handling

### Architecture

The app follows clean code principles and SOLID design patterns:

- **MainActivity.java** - UI controller following MVC pattern
- **SamsungApiClient.java** - Network layer with single responsibility
- **ApkInfo.java** - Value object for data transfer
- **Separation of Concerns** - UI, networking, and data handling are separated

### Technical Specifications

- **Minimum SDK**: API 23 (Android 6.0)
- **Target SDK**: API 36
- **Architecture**: Single Activity with proper separation of concerns
- **Network**: HTTP requests using HttpURLConnection
- **Downloads**: Android DownloadManager
- **Permissions**: Storage access for file operations

## How to Use

1. **Device Model**: Enter your Samsung device model (e.g., `SM-G950F`)
2. **SDK Version**: Enter Android SDK version (e.g., `29` for Android 10)
3. **Package Name**: Enter Samsung app package name (e.g., `com.sec.android.app.myfiles`)
4. **Tap "Download APK"**: App fetches APK information from Samsung servers
5. **Review Information**: Check version code and version name
6. **Confirm Download**: Tap "Confirm Download" to start the download
7. **Check Downloads**: APK file will be saved to your Downloads folder

## Code Structure

```
app/src/main/java/com/example/galaxyappsdownloader/
├── MainActivity.java          # Main UI controller
├── SamsungApiClient.java      # Samsung API communication
└── ApkInfo.java              # Data transfer object

app/src/main/res/
├── layout/activity_main.xml   # Main UI layout
├── values/strings.xml         # String resources
└── xml/network_security_config.xml  # Network configuration
```

## Technical Implementation Notes

### Samsung API Integration
The app uses the same Samsung API endpoint as the Python script:
```
https://vas.samsungapps.com/stub/stubDownload.as?appId={package}&deviceId={model}&mcc=425&mnc=01&csc=ILO&sdkVer={sdk}&pd=0&systemId=1608665720954&callerId=com.sec.android.app.samsungapps&abiType=64&extuk=0191d6627f38685f
```

### XML Parsing
Uses the same regex pattern as the Python script to extract:
- Result code and message
- Download URI
- Version code and name

### Error Handling
- Network connectivity issues
- Samsung server errors
- Permission denials
- Invalid responses

## Next Steps: Phase 2 - Enhanced Version

Future enhancements planned:
- First launch setup with storage folder selection
- Device model dropdown with saved models
- Input validation and smart suggestions
- Settings screen for preferences
- Enhanced UI with better design
- Offline capability with appropriate error handling

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on device/emulator

## Permissions Required

- **INTERNET**: For Samsung API requests
- **WRITE_EXTERNAL_STORAGE**: For downloading APK files (API 28 and below)
- **ACCESS_NETWORK_STATE**: For network connectivity checks

## Compatibility

- **Android 6.0+ (API 23)**: Minimum supported version
- **Samsung Devices**: Optimized for but works on any Android device
- **Network**: Requires internet connection for API requests

---

*This project replicates the functionality of the original Python script while following Android development best practices and clean code principles.*