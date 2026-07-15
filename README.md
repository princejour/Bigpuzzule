# Arrow Puzzle

A lightweight Android puzzle game built with Kotlin and Jetpack Compose.

في هذه اللعبة، يزيل اللاعب الأسهم من اللوحة وفق اتجاه كل سهم. لا يمكن إزالة السهم إذا كان سهم آخر يعترض طريقه. بعد إزالة جميع الأسهم ينتقل اللاعب تلقائيًا إلى المستوى التالي.

## Features

- Solvable 5×5 puzzle boards generated automatically
- Animated arrow movement and blocked-move feedback
- Level and move counters
- Restart button that keeps the current level number
- Responsive Jetpack Compose interface
- Offline operation with no account, analytics, advertisements, or network access

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Android Architecture Components (`ViewModel` and `StateFlow`)
- Minimum Android version: Android 7.0 / API 24
- Target SDK: API 36

## Run the project

### Android Studio

1. Install a recent stable version of Android Studio with JDK 17.
2. Clone or download this repository.
3. Open the repository folder in Android Studio.
4. Allow Gradle sync to finish and install Android SDK 36 when prompted.
5. Run the `app` configuration on an emulator or Android device.

The app does not require a Gemini key, Firebase configuration, or a `google-services.json` file.

## Build a debug APK

From Android Studio, select:

`Build` → `Build App Bundle(s) / APK(s)` → `Build APK(s)`

The generated file is normally located at:

`app/build/outputs/apk/debug/app-debug.apk`

## Project structure

- `app/src/main/java/com/example/MainActivity.kt` — game state, puzzle generation, and Compose UI
- `app/src/main/java/com/example/ui/theme/` — app colors and typography
- `app/src/main/res/` — Android resources and launcher icons
- `PRIVACY_POLICY.md` — privacy policy for users and app-store listings

## Privacy

Arrow Puzzle works offline and does not collect personal information. Read the full [Privacy Policy](PRIVACY_POLICY.md).

## License

No open-source license has been assigned yet. All rights are reserved unless the repository owner adds a license file.
