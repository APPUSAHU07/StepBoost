# StepBoost 🚶‍♂️⚡
### Google Health Connect Daily Step Booster & Manager (No Root Required)

**StepBoost** is an Android application that alters and injects daily step counts into **Google Health Connect** with realistic, human-like cadence patterns. Because it uses Android's official Health Connect Client SDK, **no rooting or system modification is required**.

Once steps are written to Health Connect, any fitness app linked to Health Connect (such as **Google Fit**, **Samsung Health**, **MyFitnessPal**, **Strava**, etc.) automatically reads and syncs the updated daily steps.

---

## ✨ Features

- **🛡️ 100% No-Root Required**: Operates using Google's official `androidx.health.connect` SDK with standard user-level runtime permissions.
- **🏃 Natural Human Cadence Engine**:
  - Automatically avoids unrealistic spikes (e.g., 10,000 steps in 1 minute) that could trigger flags in tracking or reward apps.
  - Distributes steps at natural walking cadences (95–125 steps/minute) across realistic sessions throughout active day hours.
- **⚡ Instant Quick Boost**:
  - One-tap buttons to add `+500`, `+1,000`, `+2,500`, `+5,000` steps as a natural recent walk.
  - **"Top up to Goal"**: Calculates how many steps are needed to reach your daily goal (e.g., 10,000) and smoothly distributes them across today.
- **🤖 Background Auto-Faker**:
  - Runs in the background via Android `WorkManager` (every 1–2 hours).
  - Automatically injects 300–650 steps at random intervals during the day.
  - Automatically pauses at night (after 9:00 PM) to maintain realistic activity patterns.
  - Stops automatically once today's step goal is reached.
- **🎨 Custom Step Generator**:
  - Interactive slider to inject any custom amount (up to 30,000 steps).
  - Choice between **Natural Day Spacing** (distributed from 8:00 AM to now) or **Single Recent Walk**.
- **📊 Real-time Step Counter & Log**:
  - Shows today's real-time step aggregate from Health Connect.
  - Displays each individual session with timestamp ranges and the ability to delete any session.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android 9+ (API 28+)
- **Language**: Kotlin 2.0 with Coroutines
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Health API**: `androidx.health.connect:connect-client:1.1.0-alpha11`
- **Background Tasks**: `androidx.work:work-runtime-ktx` (WorkManager)
- **Permissions**: `android.permission.health.READ_STEPS`, `android.permission.health.WRITE_STEPS`

---

## 🚀 How to Build & Install on Your Phone

### Option 1: Using Android Studio (Easiest)
1. Open **Android Studio**.
2. Select **File → Open...** and choose the folder:
   ```
   d:\work\StepBoost
   ```
3. Allow Gradle to sync dependencies.
4. Enable **Developer Options** and **USB Debugging** on your Android phone, then plug it in via USB (or pair via Wireless Debugging).
5. Select your phone in the device dropdown and click the green **Run (▶)** button.
6. Android Studio will install and open StepBoost on your phone.

---

### Option 2: Build the APK from Terminal
Run the Gradle wrapper inside this project:
```bash
./gradlew assembleDebug
```
(On Windows Command Prompt or PowerShell: `.\gradlew.bat assembleDebug`)

Once built, your APK will be ready at:
```
app/build/outputs/apk/debug/app-debug.apk
```
You can send this APK to your phone (via WhatsApp, Google Drive, USB transfer, or `adb install app-debug.apk`) and tap to install it.

---

## 📱 First-Time Setup on Your Phone

1. **Open StepBoost**:
   - You will see the Health Connect status banner.
2. **Grant Permissions**:
   - Tap **"Grant Permissions"**.
   - Android will open the official Health Connect permission screen.
   - Toggle **"Allow all"** (or toggle *Read steps* and *Write steps*), then tap **Allow**.
3. **Connect Your Favorite Fitness Apps**:
   - Open **Health Connect** on your phone (in your phone's *Settings → Health Connect*, or via the Health Connect app).
   - Go to **Data and access → Connected apps**.
   - Make sure your fitness apps (e.g., **Google Fit**, **Samsung Health**, etc.) are granted permission to read steps from Health Connect.
4. **Boost Your Steps**:
   - Tap any Quick Boost button (e.g. `+1000` or `Top up to Goal`) or toggle **Background Auto-Faker**.
   - Open Google Fit or Samsung Health and refresh — your new steps appear immediately!
