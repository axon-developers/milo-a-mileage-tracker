<div align="center">

# 🚗 Milo — A Mileage Tracker

**Track fuel fill-ups, monitor efficiency trends, and manage your vehicle fleet — all offline, all private.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.7.0-3DDC84?logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange)](https://developer.android.com/about/versions/oreo)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen)](https://developer.android.com/about/versions)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

---

## 📱 About

**Milo** is a fully offline Android mileage and fuel tracking application built with modern Android development practices. It helps vehicle owners:

- **Log every fuel fill-up** with odometer, volume, price, cost, and location
- **Calculate mileage efficiency** automatically (km/L, MPG, mi/gal)
- **Visualize trends** with built-in bar and line charts — no third-party chart library needed
- **Manage multiple vehicles** of various types (sedan, SUV, motorcycle, truck, van)
- **Stay private** — all data is stored locally using Room (SQLite), nothing is sent to the cloud

---

## ✨ Features

### 🏠 Dashboard
- Personalized greeting with total fleet stats
- Summary cards: total spend, total distance, average efficiency
- Monthly spend comparison with previous month (% change indicator)
- Per-vehicle quick-access tiles with latest efficiency reading
- Floating action button to quickly log a fill-up

### 📋 All Logs
- Chronological fuel entry log across all vehicles
- Per-entry detail: odometer, volume, cost, price-per-unit, location, efficiency
- Inline edit and delete for every entry
- Add new entries from anywhere

### 📊 Analytics
- **Monthly Fuel Spend Bar Graph** — last 6 calendar months, aggregate across all vehicles
- **Per-vehicle Mileage Efficiency Trend** — line graph with data point labels
- Vehicle selector dropdown to compare efficiency across your fleet

### ⚙️ Settings
- **Theme** — Light / Dark / System
- **Currency** — $, €, £, ₹, ¥
- **Distance Unit** — Kilometers (km) / Miles (mi)
- **Volume Unit** — Liters (L) / Gallons (gal)
- **Dashboard Customization** — toggle Monthly Spend card and 6-month graph visibility

### 🚗 Vehicle Management
- Register vehicles with name, type, fuel type, and initial odometer
- Edit and delete vehicles
- Per-vehicle transaction history with efficiency trend chart
- Supported types: Sedan, SUV, Motorcycle, Truck, Van
- Supported fuel types: Petrol, Diesel, Electric, Hybrid, LPG

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM with `StateFlow` |
| **Database** | Room 2.7.0 (SQLite, offline-first) |
| **DI** | Manual factory (`FuelViewModelFactory`) |
| **Charts** | Custom Canvas-drawn bar & line graphs |
| **Navigation** | Bottom navigation with 4 tabs |
| **Preferences** | `DataStore` (Preferences) |
| **Build** | Gradle 8.x with KSP |
| **Language** | Kotlin 2.2.10 |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 36 |

---

## 📂 Project Structure

```
milo-a-mileage-tracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/axon/milo/
│   │   │   ├── data/
│   │   │   │   ├── FuelDatabase.kt       # Room database
│   │   │   │   ├── FuelEntry.kt          # Fuel log entity
│   │   │   │   ├── FuelEntryDao.kt       # DAO for fuel entries
│   │   │   │   ├── Vehicle.kt            # Vehicle entity
│   │   │   │   └── VehicleDao.kt         # DAO for vehicles
│   │   │   ├── repository/
│   │   │   │   └── FuelRepository.kt     # Data layer abstraction
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   └── MainDashboard.kt  # All composable screens
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   └── viewmodel/
│   │   │   │       ├── FuelViewModel.kt
│   │   │   │       └── FuelViewModelFactory.kt
│   │   │   └── MainActivity.kt
│   │   └── res/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                # Centralized dependency versions
├── .env.example                          # API key template
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer — [Download](https://developer.android.com/studio)
- **JDK 17+** (bundled with Android Studio)
- **Android SDK** with API Level 26+ installed
- **Android Emulator** or a physical device running Android 8.0+

### Local Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/milo-a-mileage-tracker.git
   cd milo-a-mileage-tracker
   ```

2. **Create your `.env` file**

   Copy the example and add your key (used for optional AI features):
   ```bash
   cp .env.example .env
   ```
   Open `.env` and set:
   ```
   GEMINI_API_KEY=your_actual_key_here
   ```

3. **Open in Android Studio**

   - Launch Android Studio
   - Click **Open** → select the `milo-a-mileage-tracker` directory
   - Wait for Gradle sync to complete

4. **Run the app**

   - Connect an Android device or start an emulator (API 26+)
   - Click ▶ **Run** in Android Studio

### Build from Command Line

**Debug APK:**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Release APK** (requires signing config):
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

**Release AAB** (for Google Play):
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## 🔑 Environment Variables

The app uses the `secrets-gradle-plugin` to read sensitive keys from `.env`:

| Variable | Description | Required |
|----------|-------------|----------|
| `GEMINI_API_KEY` | Google Gemini AI API key | Optional (for AI features) |

> ⚠️ Never commit your `.env` file. It is already listed in `.gitignore`.

---

## 📊 Data Model

### Vehicle
| Field | Type | Description |
|-------|------|-------------|
| `id` | Int (PK, auto) | Unique identifier |
| `name` | String | Display name |
| `type` | String | Sedan / SUV / Motorcycle / Truck / Van |
| `fuelType` | String | Petrol / Diesel / Electric / Hybrid / LPG |
| `initialOdometer` | Double | Starting odometer reading |

### FuelEntry
| Field | Type | Description |
|-------|------|-------------|
| `id` | Int (PK, auto) | Unique identifier |
| `vehicleId` | Int (FK) | Links to Vehicle |
| `timestamp` | Long | Unix timestamp of fill-up |
| `odometer` | Double | Odometer at fill-up |
| `litersFilled` | Double | Volume of fuel filled |
| `pricePerLiter` | Double | Price per unit of fuel |
| `totalCost` | Double | Total cost paid |
| `location` | String? | Optional fill-up location |

**Efficiency Calculation:**
```
efficiency = (current_odometer - previous_odometer) / volume_filled
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  MainDashboard.kt (Compose Screens + Navigation)    │
│  • DashboardScreen   • LogFuelScreen                │
│  • AnalyticsScreen   • SettingsScreen               │
│  • VehicleDetailScreen + Dialogs                    │
└───────────────────┬─────────────────────────────────┘
                    │ StateFlow / collectAsState
┌───────────────────▼─────────────────────────────────┐
│                 ViewModel Layer                     │
│  FuelViewModel.kt                                   │
│  • Exposes UI state via StateFlow                   │
│  • Orchestrates business logic                      │
│  • Manages DataStore preferences                    │
└───────────────────┬─────────────────────────────────┘
                    │ suspend functions
┌───────────────────▼─────────────────────────────────┐
│               Repository Layer                      │
│  FuelRepository.kt                                  │
│  • Single source of truth                           │
│  • Abstracts DAO operations                         │
└───────────────────┬─────────────────────────────────┘
                    │ Flow / coroutines
┌───────────────────▼─────────────────────────────────┐
│                  Data Layer                         │
│  Room Database (FuelDatabase.kt)                    │
│  • VehicleDao   • FuelEntryDao                      │
│  • SQLite (local, offline)                          │
└─────────────────────────────────────────────────────┘
```

---

## 📦 Building for Release

### 1. Create a Signing Keystore

```bash
keytool -genkey -v -keystore milo-release.jks \
  -alias milo \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. Configure Signing in `app/build.gradle.kts`

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("milo-release.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "milo"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

### 3. Build

```bash
# Release APK
./gradlew assembleRelease

# Release AAB (recommended for Play Store)
./gradlew bundleRelease
```

---

## 🧪 Testing

The app includes `testTag` annotations on all interactive UI elements for automated UI testing:

```kotlin
// Example test tags
"dashboard_add_log_fab"          // Main FAB
"save_vehicle_button"            // Save vehicle in dialog
"add_entry_dialog_odometer"      // Odometer input
"submit_add_entry_dialog_button" // Submit fuel entry
"theme_selector_button"          // Theme toggle
```

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests (requires device/emulator):
```bash
./gradlew connectedAndroidTest
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/add-export`
3. Commit your changes: `git commit -m 'Add CSV export'`
4. Push to the branch: `git push origin feature/add-export`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

<div align="center">
Made with ❤️ using Jetpack Compose · Room · Kotlin
</div>
