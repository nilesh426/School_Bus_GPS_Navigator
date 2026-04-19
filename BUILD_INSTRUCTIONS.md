# 🔨 BUILD & DEPLOYMENT INSTRUCTIONS

## 📋 PRE-BUILD CHECKLIST

Before building, verify:
- [ ] Android Studio is open with project
- [ ] All files are created (check FILES_CREATED_MODIFIED.md)
- [ ] No syntax errors in new files
- [ ] Git (optional) - commit changes

## 🏗️ BUILDING THE PROJECT

### Option 1: Using Android Studio (Recommended)

1. **Open Android Studio**
   - Open the SchoolBusApp project

2. **Sync Gradle**
   - Top menu: File → Sync Now
   - Wait for gradle sync to complete

3. **Build**
   - Top menu: Build → Rebuild Project
   - Or press: Ctrl+F9

4. **Verify Success**
   - Check Build Console at bottom
   - Should see: "Build completed successfully"

### Option 2: Using Terminal/Command Line

```bash
# Navigate to project directory
cd C:\Users\Nilesh\ Sawant\AndroidStudioProjects\SchoolBusApp

# Clean previous build
./gradlew clean

# Build project
./gradlew build

# Expected output: BUILD SUCCESSFUL
```

### Option 3: Using PowerShell (Windows)

```powershell
# Navigate to project
cd "C:\Users\Nilesh Sawant\AndroidStudioProjects\SchoolBusApp"

# Build
.\gradlew build

# Watch for: BUILD SUCCESSFUL
```

## ✅ SUCCESSFUL BUILD INDICATORS

You'll see:
```
> Task :app:preBuild
> Task :app:preDebugBuild
> Task :app:compileDebugKotlin
> Task :app:compileDebugJava
...
> Task :app:assembleDebug

BUILD SUCCESSFUL in Xs
```

## ❌ COMMON BUILD ERRORS & FIXES

### Error 1: "Cannot find symbol: NotificationHelper"
**Cause:** NotificationHelper.kt not created  
**Fix:** Create the file: `app/src/main/java/com/example/schoolbusapp/NotificationHelper.kt`

### Error 2: "Cannot find symbol: StopDetectionManager"
**Cause:** StopDetectionManager.kt not created  
**Fix:** Create the file: `app/src/main/java/com/example/schoolbusapp/StopDetectionManager.kt`

### Error 3: "Gradle sync failed"
**Cause:** Gradle cache issue  
**Fix:**
```bash
./gradlew clean
./gradlew build
```

### Error 4: "SDK version not found"
**Cause:** Target SDK mismatch  
**Fix:** Update build.gradle.kts targetSdk to 34

### Error 5: "Android permission not found"
**Cause:** POST_NOTIFICATIONS not added to manifest  
**Fix:** Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 🧪 TESTING AFTER BUILD

### Quick Sanity Test (2 minutes)

1. **Run on Emulator**
   ```
   Top menu: Run → Run 'app'
   Or press: Shift+F10
   ```

2. **Verify App Launches**
   - App should start without crashing
   - Should see splash screen
   - Should see login screen

3. **Check No Crashes**
   - Logcat should show no errors
   - Filter by: com.example.schoolbusapp

### Full Test (30 minutes)

Follow: **STOP_NOTIFICATION_TESTING.md**

## 📦 BUILDING FOR RELEASE

### Step 1: Update Version
Edit `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 2  // Increment
        versionName = "2.0.0"  // Update
    }
}
```

### Step 2: Build Release APK
```bash
./gradlew assembleRelease
```

Output location: `app/build/outputs/apk/release/app-release-unsigned.apk`

### Step 3: Build Release Bundle (for Play Store)
```bash
./gradlew bundleRelease
```

Output location: `app/build/outputs/bundle/release/app-release.aab`

### Step 4: Sign APK/Bundle
(Requires keystore - see Android documentation)

### Step 5: Deploy
Upload to Play Store or distribute directly

## 📊 BUILD PERFORMANCE

### Expected Build Times
- First build: 2-3 minutes (includes downloading)
- Incremental build: 30-60 seconds
- Clean build: 1-2 minutes

### If Build is Slow
1. Increase Gradle JVM memory:
   - File → Settings → Build, Execution, Deployment → Compiler → Gradle VM Options
   - Add: `-Xmx2048m`

2. Enable Gradle offline mode (if all deps downloaded):
   - File → Settings → Build, Execution, Deployment → Gradle
   - Check: "Offline work"

## 📝 BUILD OUTPUT STRUCTURE

After successful build:
```
app/build/
├── intermediates/
│   ├── compiled_classes/
│   ├── manifests/
│   ├── res/
│   └── ...
├── outputs/
│   ├── apk/
│   │   ├── debug/
│   │   │   └── app-debug.apk  ← Use for testing
│   │   └── release/
│   │       └── app-release-unsigned.apk
│   ├── bundle/
│   │   └── release/
│   │       └── app-release.aab  ← Use for Play Store
│   └── ...
└── reports/
    ├── lint/
    └── ...
```

## 🚀 DEPLOYING FOR TESTING

### Deploy to Emulator

1. **Select Emulator**
   - Top right: Select device dropdown
   - Choose emulator (e.g., "Pixel 4 API 30")

2. **Run App**
   - Top menu: Run → Run 'app'
   - Or press: Shift+F10

3. **Wait for Installation**
   - Shows progress in bottom panel
   - Watch for: "App successfully installed"

### Deploy to Physical Device

1. **Enable USB Debugging on Device**
   - Settings → Developer Options → USB Debugging

2. **Connect Device via USB**

3. **Verify Device Recognized**
   ```bash
   adb devices
   ```
   Should list your device

4. **Run App**
   - Top menu: Run → Run 'app'
   - Or press: Shift+F10
   - Select device when prompted

## 🧹 CLEAN BUILD

If you encounter build issues:

```bash
# Option 1: Clean and rebuild
./gradlew clean build

# Option 2: Full clean with cache clear
./gradlew clean
./gradlew build --refresh-dependencies

# Option 3: In Android Studio
# File → Invalidate Caches → Invalidate and Restart
```

## 🔍 VERIFYING BUILD SUCCESS

### Build Console Check
```
✅ Look for: "BUILD SUCCESSFUL in XXs"
❌ Avoid: "BUILD FAILED"
```

### Logcat Check
```bash
adb logcat | grep "SchoolBusApp\|ERROR"
```

Should NOT show errors related to:
- NotificationHelper
- StopDetectionManager
- DriverRouteMapActivity
- AndroidManifest

## 📱 APK INSTALLATION VERIFICATION

After building and deploying:

1. **Check App Exists**
   - Device Settings → Apps
   - Should see "School Bus App"

2. **Check App Runs**
   - Tap app icon
   - Should see splash screen → login screen

3. **Check No Crashes**
   - Keep app open for 10 seconds
   - Should not crash
   - No error dialogs

4. **Check Permissions Prompt**
   - Go to DriverRouteMapActivity
   - Should prompt for notification permission (Android 13+)
   - Grant permission
   - Should not crash

## ⚡ GRADLE CACHING

To speed up builds:

**Enable Gradle Daemon** (faster):
```bash
echo "org.gradle.daemon=true" >> gradle.properties
```

**Disable if needed** (debugging):
```bash
./gradlew build --no-daemon
```

**Clear Gradle cache** (if corrupted):
```bash
./gradlew clean --refresh-dependencies
```

## 🐛 DEBUGGING BUILD ISSUES

### Enable Verbose Output
```bash
./gradlew build --debug
```

### Check Gradle Version
```bash
./gradlew --version
```

### Check Java Version
```bash
java -version
```

Should be Java 11 or higher.

### Update Gradle
Edit: `gradle/wrapper/gradle-wrapper.properties`
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-all.zip
```

## 📋 BUILD CHECKLIST

### Before Building
- [ ] All 2 new Kotlin files created
- [ ] 2 code files modified correctly
- [ ] AndroidManifest.xml updated
- [ ] No unsaved changes

### After Building
- [ ] Build shows "SUCCESS"
- [ ] No compilation errors
- [ ] APK/Bundle generated
- [ ] Can run on emulator/device
- [ ] App doesn't crash on startup

## 🎯 TROUBLESHOOTING BUILDS

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Run: `./gradlew clean` |
| Cannot find imports | Check files created in correct path |
| Symbol not found | Check class names are exact |
| Version conflict | Update gradle to latest version |
| Out of memory | Increase Gradle JVM memory |
| Long build time | Enable Gradle daemon |

## 📞 GETTING HELP

### Build Failed?
1. Check error message in Build Console
2. Search error in Android documentation
3. Try clean build: `./gradlew clean build`
4. Restart Android Studio

### Still Stuck?
1. Check all files created (see FILES_CREATED_MODIFIED.md)
2. Verify file contents match
3. Check Android Manifest has new permission
4. Verify no typos in new code

## 🚀 AFTER SUCCESSFUL BUILD

1. **Run Tests** (optional)
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. **Deploy to Device**
   ```bash
   ./gradlew installDebug
   ```

3. **Run App**
   - Open app on emulator/device
   - Follow testing guide

4. **Verify Features**
   - Test stop notification
   - Check all scenarios work

## 📊 BUILD STATISTICS

Expected metrics:
- **Build time:** 1-2 minutes
- **APK size:** ~5-10 MB
- **Dex files:** 1-2
- **Method count:** ~50,000-65,000
- **Resource files:** ~500+

## ✅ FINAL VERIFICATION

```bash
# Everything built and ready?
./gradlew assembleDebug

# Check APK exists
ls app/build/outputs/apk/debug/

# Success!
# Ready for testing: STOP_NOTIFICATION_TESTING.md
```

---

**Build Instructions Complete!**  
**Next Step:** Test the feature (STOP_NOTIFICATION_TESTING.md)  
**Build Time:** ~2 minutes  
**Status:** ✅ READY TO BUILD

