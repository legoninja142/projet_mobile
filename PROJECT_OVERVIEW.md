# Sleep Tracking Android Application - Project Overview

## Project Summary
This is an Android application for tracking sleep records. Built with Java and Kotlin, using Gradle as the build system and Firebase (Firestore) for backend data storage. The app follows a modular architecture with separation between models, controllers, UI, and views.

**Target SDK:** Android 36 | **Minimum SDK:** 24 | **Java Version:** 11

---

## Directory Structure

```
/youssef (Project Root)
├── app/                                    # Main Android application module
│   ├── build.gradle.kts                   # Module-level Gradle configuration
│   ├── google-services.json               # Firebase configuration
│   ├── proguard-rules.pro                 # ProGuard rules for release builds
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # App manifest with permissions & activities
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── MainActivity.java      # Main entry point activity
│   │   │   │   ├── model/                 # Data models
│   │   │   │   │   ├── SleepRecord.java   # Sleep record entity (Firestore-compatible)
│   │   │   │   │   └── User.java          # User entity
│   │   │   │   ├── controller/            # Business logic layer
│   │   │   │   │   ├── LoginController.java  # Authentication logic
│   │   │   │   │   └── SessionManager.java   # Session management
│   │   │   │   ├── ui/                    # UI components (fragments, dialogs, etc.)
│   │   │   │   └── view/                  # Custom views or view utilities
│   │   │   └── res/                       # Android resources
│   │   │       ├── layout/                # XML layout files
│   │   │       ├── drawable/              # Images and vector drawables
│   │   │       └── values/                # Strings, colors, dimensions
│   │   ├── androidTest/                   # Android instrumentation tests
│   │   └── test/                          # Unit tests
│   └── build/                             # Build output (generated files, intermediates, outputs)
│
├── gradle/
│   ├── libs.versions.toml                 # Centralized dependency versions
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                       # Project-level Gradle configuration
├── settings.gradle.kts                    # Gradle settings (module inclusion)
├── gradle.properties                      # Gradle build properties
├── gradlew / gradlew.bat                  # Gradle wrapper scripts
├── local.properties                       # Local SDK/NDK paths
└── .git/                                  # Version control

```

---

## Key Files & Components

### 1. **Data Models** (`app/src/main/java/com/example/myapplication/model/`)

#### `SleepRecord.java`
- **Purpose:** Represents a manually logged sleep session
- **Firestore Compatible:** Has no-argument constructor required by Firestore
- **Fields:**
  - `id` (String) — Unique Firestore document ID
  - `userId` (String) — User who logged the sleep
  - `sleepStartTime` (Date) — When sleep started
  - `sleepEndTime` (Date) — When sleep ended
  - `sleepDuration` (int, minutes) — Auto-calculated from start/end times
  - `sleepQuality` (int, 1-5 scale) — User-rated quality of sleep (5 = best)
  - `notes` (String) — Optional user notes about sleep quality
  - `recordDate` (Date) — Timestamp when record was created
- **Auto-Calculations:** `sleepDuration` is automatically calculated in minutes from difference between `sleepEndTime` and `sleepStartTime`

#### `SleepSensorData.java`
- **Purpose:** Contains sensor-based sleep analysis data
- **Fields:**
  - `id` (String) — Unique record ID
  - `userId` (String) — User associated with monitoring session
  - `sessionStartTime` (Date) — When sensor monitoring started
  - `sessionEndTime` (Date) — When sensor monitoring stopped
  - `movementData` (List<MovementData>) — Accelerometer readings throughout session
  - `lightData` (List<LightData>) — Light sensor readings throughout session
  - `estimatedSleepQuality` (int, 1-5) — AI-estimated quality based on patterns
  - `averageMovementPerHour` (float) — Average movement intensity per hour
  - `averageLightLevel` (float) — Average light exposure in lux
  - `wasDarkEnvironment` (boolean) — Whether environment was dark (< 10.0 lux)

#### `MovementData.java`
- Represents accelerometer reading with timestamp, acceleration magnitude, and movement intensity (1-5)

#### `LightData.java`
- Represents light sensor reading with timestamp, light level in lux, and dark/light boolean

#### `User.java`
- **Purpose:** Represents application user entity
- Contains user profile information, authentication details, and preferences

### 2. **ViewModel Layer** (`app/src/main/java/com/example/myapplication/ui/sleep/`)

#### `SleepViewModel.java`
- **Purpose:** Lifecycle-aware component managing UI data and business logic
- **LiveData Streams:**
  - `sleepRecords` — List of user's sleep records
  - `isLoading` — Loading state indicator
  - `errorMessage` — Error messages for UI
  - `selectedRecord` — Currently selected record
- **Key Methods:**
  - `loadSleepRecords(userId)` — Fetch all records for a user
  - `addSleepRecord(record, userId)` — Create new sleep record
  - `updateSleepRecord(recordId, record)` — Modify existing record
  - `deleteSleepRecord(recordId, userId)` — Remove record

### 3. **Repository Layer** (`app/src/main/java/com/example/myapplication/repository/`)

#### `SleepRepository.java`
- **Purpose:** Data access object handling Firestore operations
- **Firestore Collection:** `sleep_records`
- **Key Methods:**
  - `addSleepRecord()` — Creates new sleep record
  - `getSleepRecordsByUser()` — Retrieves user's records (sorted by date DESC)
  - `updateSleepRecord()` — Modifies existing record
  - `deleteSleepRecord()` — Removes record
  - `getSleepRecord()` — Fetches single record by ID

### 4. **Service Layer** (`app/src/main/java/com/example/myapplication/service/`)

#### `SleepSensorService.java`
- **Purpose:** Android Foreground Service for continuous sensor monitoring
- **Sensors Used:**
  - **Accelerometer** (TYPE_ACCELEROMETER) — Detects movement patterns (5-second intervals, 1.5 m/s² threshold)
  - **Light Sensor** (TYPE_LIGHT) — Monitors ambient light levels (30-second intervals, 10.0 lux threshold)
- **Key Methods:**
  - `onCreate()` — Initializes sensors and system resources
  - `onStartCommand()` — Routes start/stop actions
  - `startSleepMonitoring()` — Begins sensor monitoring with WakeLock
  - `stopSleepMonitoring()` — Halts monitoring and saves data
  - `onSensorChanged()` — Processes sensor events and buffers data
  - `checkAndSaveData()` — Periodic task (every 5 minutes) to save sensor data
- **Features:**
  - Keeps CPU active with PARTIAL_WAKE_LOCK
  - Foreground notification for user awareness
  - Periodic Firestore saves (every 5 minutes)
  - Batch processing of sensor data

### 5. **UI Layer** (`app/src/main/java/com/example/myapplication/ui/sleep/`)

#### `SleepFragment.java`
- **Purpose:** Main UI for sleep tracking functionality
- **Features:**
  - Date/Time pickers for sleep session times (default: yesterday 22:00 → today 07:00)
  - Sleep quality spinner (1-5 scale)
  - Notes input field
  - RecyclerView list of recorded sleep sessions
  - Sensor tracking start/stop buttons
  - Real-time duration calculation
  - Permission management for sensors
- **Key Methods:**
  - `onViewCreated()` — Setup UI components and observers
  - `saveSleepRecord()` — Validate and save manual sleep entry
  - `startSensorTracking()` — Initiate sensor monitoring service
  - `stopSensorTracking()` — Halt sensor monitoring
  - `checkPermissions()` / `requestPermissions()` — Manage sensor permissions

#### `SleepRecordAdapter.java`
- **Purpose:** RecyclerView adapter for displaying sleep records
- **Features:** Bind sleep data to list items, handle edit/delete actions

### 6. **Business Logic Layer** (`app/src/main/java/com/example/myapplication/controller/`)

#### `LoginController.java`
- Handles user authentication workflows
- Integration with Firebase Authentication

#### `SessionManager.java`
- Manages user sessions and authentication state
- Handles user login/logout flows

### 7. **Entry Point**
- `MainActivity.java` — Primary activity that launches when app starts

---

## Build Configuration

### **app/build.gradle.kts** (Module-level)
- **Plugins:** Android Application, Google Services (Firebase)
- **Namespace:** `com.example.myapplication`
- **Compile/Target SDK:** 36
- **Key Features:**
  - View Binding enabled
  - Java 11 source/target compatibility
  - Firebase Firestore integration
  - Material Design dependencies
  - Android Navigation component
  - Lifecycle & ViewModel support

### **Key Dependencies:**
- **AndroidX:** AppCompat, Material, ConstraintLayout
- **Lifecycle:** LiveData, ViewModel
- **Navigation:** Fragment & UI navigation
- **Firebase:** Analytics, Firestore
- **Testing:** JUnit, Espresso (instrumented), MockK

### **gradle/libs.versions.toml**
- Centralized version management for all dependencies
- Ensures consistent version usage across modules

---

## Development Environment

### **Required Tools:**
- **IDE:** Android Studio (Otter | 2025.2.1 Patch 1 or later)
- **JDK:** Java 11 or higher
- **Android SDK:** API level 36 (target), API level 24 (minimum)
- **Gradle:** 8.x (wrapper included)

### **Firebase Setup:**
- `google-services.json` must be present in `app/` directory
- Configure Firebase project in Firebase Console
- Enable Firestore Database and Authentication services

---

## Build & Run Instructions

### **From Terminal:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run with live logging
./gradlew installDebug -v
```

### **From Android Studio:**
1. Open the project in Android Studio
2. Select a device/emulator in the device manager
3. Click **Run** or press `Shift + F10`

### **Run Tests:**
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

---

## Architecture Overview

### MVVM Architecture Pattern

The application follows the **Model-View-ViewModel (MVVM)** architecture with a **Repository Pattern** for data abstraction:

```
┌─────────────────────────────────────────────────────────┐
│                   UI Layer (View)                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │  SleepFragment                                    │  │
│  │  - Date/Time Pickers, Quality Spinner, Notes     │  │
│  │  - RecyclerView (Sleep Records List)             │  │
│  │  - Sensor Tracking Control Buttons               │  │
│  └───────────────────────────────────────────────────┘  │
│                        │                                 │
│                        │ (LiveData Observers)            │
│                        ▼                                 │
│  ┌───────────────────────────────────────────────────┐  │
│  │  SleepViewModel                                   │  │
│  │  - sleepRecords: MutableLiveData<List>            │  │
│  │  - isLoading, errorMessage, selectedRecord       │  │
│  │  - loadSleepRecords(), addSleepRecord(),          │  │
│  │    updateSleepRecord(), deleteSleepRecord()       │  │
│  └───────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ (Repository Calls)
                         ▼
┌─────────────────────────────────────────────────────────┐
│            Repository & Service Layer                   │
│  ┌───────────────────────────────────────────────────┐  │
│  │  SleepRepository                                  │  │
│  │  - addSleepRecord()                               │  │
│  │  - getSleepRecordsByUser()                        │  │
│  │  - updateSleepRecord()                            │  │
│  │  - deleteSleepRecord()                            │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  SleepSensorService                               │  │
│  │  - startSleepMonitoring()                         │  │
│  │  - stopSleepMonitoring()                          │  │
│  │  - Sensor data collection & analysis              │  │
│  │  - Periodic data saving to Firestore              │  │
│  └───────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ (Firestore Queries)
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Data Layer (Models)                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  SleepRecord - Manual sleep entries               │  │
│  │  SleepSensorData - Sensor-based tracking          │  │
│  │  MovementData - Accelerometer readings            │  │
│  │  LightData - Light sensor readings                │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │   Firebase Firestore Database  │
        │   Collection: sleep_records    │
        └────────────────────────────────┘
```

### Key Architecture Features

- **Separation of Concerns:** Models, ViewModels, Repositories, and UI are logically separated
- **Reactive Data Flow:** LiveData provides real-time updates to UI
- **Async Operations:** Repository handles async Firestore queries with callbacks
- **Service Independence:** SleepSensorService runs independently in background
- **User Session Management:** SessionManager tracks authentication state across app lifecycle

---

## Core Features

### ✅ Manual Sleep Logging
- **Date/Time Pickers** — Flexible entry of sleep times (supports overnight sleep)
- **Sleep Quality Rating** — 1-5 scale for user subjective assessment
- **Notes Field** — Optional comments about sleep session
- **Auto-Calculation** — System calculates duration in minutes
- **Validation** — Ensures end time is after start time

### ✅ Sensor-Based Sleep Tracking
- **Accelerometer Monitoring** — Detects movement patterns (5-second sampling)
- **Light Level Tracking** — Monitors ambient light (30-second sampling)
- **Background Service** — Runs continuously with WakeLock
- **Automatic Saving** — Data saved every 5 minutes to Firestore
- **Sleep Quality Estimation** — AI-based quality prediction from sensor data

### ✅ Sleep Records Management
- **List View** — Display all recorded sleep sessions (sorted by date)
- **CRUD Operations** — Create, read, update, delete records
- **Record Details** — Shows date, duration, quality at a glance
- **Deletion** — Remove unwanted records from database
- **Firestore Persistence** — Cloud storage with user-specific access

### ✅ User Authentication
- **Firebase Auth Integration** — Secure user login/signup
- **Session Persistence** — Maintain login state across app restarts
- **User-Specific Data** — Sleep records filtered by authenticated user

---

## Data Storage

### Firestore Collections

**Collection: `sleep_records`**
- Stores manually logged sleep sessions
- Documents indexed by userId for efficient querying
- Sorted by recordDate (descending) for reverse chronological display

**Collection: `sleep_sensor_data`** (Optional)
- Stores sensor monitoring session data
- Contains detailed movement and light readings
- Used for advanced analytics and insights

### Query Patterns

```firestore
// Load user's sleep records
db.collection("sleep_records")
  .whereEqualTo("userId", currentUser)
  .orderBy("recordDate", DESCENDING)
  .get()

// Add new sleep record
db.collection("sleep_records").add(sleepRecord)

// Update existing record
db.collection("sleep_records").document(docId).set(updatedRecord)

// Delete record
db.collection("sleep_records").document(docId).delete()
```

---

## Development Workflow

1. **Understand the Architecture** — Review MVVM pattern and data flow
2. **Create/Modify Data Models** in `model/` for new data structures
3. **Implement Business Logic** in `repository/` for database operations
4. **Create ViewModel** in `ui/` to manage UI state and data
5. **Build UI** in `ui/sleep/` with Fragments for user interactions
6. **Implement Services** in `service/` for background operations
7. **Test Thoroughly** using unit tests (`test/`) and instrumented tests (`androidTest/`)
8. **Configure Firebase** via `google-services.json` in app root
9. **Build & Deploy** via Gradle or Android Studio

---

## Permissions & Sensor Configuration

### Required Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

- **ACTIVITY_RECOGNITION** — Runtime permission (Android 10+) for accelerometer access
- **WAKE_LOCK** — Install-time permission to keep CPU active during monitoring

### Sensor Thresholds

```java
MOVEMENT_THRESHOLD = 1.5f              // m/s² (accelerometer)
LIGHT_DARK_THRESHOLD = 10.0f           // Lux (light sensor)
ACCELEROMETER_INTERVAL = 5000          // 5 seconds
LIGHT_SENSOR_INTERVAL = 30000          // 30 seconds
SAVE_INTERVAL = 300000                 // 5 minutes (300,000 ms)
```

### WakeLock Configuration

- **Type:** PARTIAL_WAKE_LOCK (keeps CPU awake, doesn't turn on screen)
- **Tag:** "SleepTrackerApp::SleepSensorWakeLock"
- **Purpose:** Maintain sensor monitoring during deep sleep

---

## Troubleshooting Guide

### Sensor Tracking Not Working
1. ✓ Verify ACTIVITY_RECOGNITION permission is granted
2. ✓ Ensure device has accelerometer (check device specs)
3. ✓ Check battery optimization settings (allow app to run in background)
4. ✓ Review logcat for SleepSensorService errors
5. ✓ Restart app and try again

### Sleep Records Not Saving
1. ✓ Verify internet connection
2. ✓ Check Firestore database rules allow write access
3. ✓ Confirm user is authenticated (check SessionManager)
4. ✓ Look at logcat for Firestore exceptions
5. ✓ Verify google-services.json is correctly configured

### Permissions Not Requested
1. ✓ Check if running on Android 10+ (runtime permissions required)
2. ✓ Clear app cache (Settings > Apps > Sleep Tracker > Storage > Clear Cache)
3. ✓ Verify permissions defined in AndroidManifest.xml
4. ✓ Reinstall app if needed

### Battery Draining Quickly
1. ✓ Only use sensor tracking when necessary
2. ✓ Disable light sensor if battery is critical
3. ✓ Increase sampling intervals if acceptable
4. ✓ Use devices with efficient sensors

### Firestore Queries Failing
1. ✓ Check Firestore database exists in Firebase Console
2. ✓ Verify composite indexes for user queries are created
3. ✓ Ensure Firestore security rules allow collection reads
4. ✓ Test queries in Firebase Console Firestore UI

---

## Performance Optimization

### Battery Usage
- WakeLock keeps CPU active (significant battery drain)
- Sensor sampling throttled to 5-30 second intervals
- Batch processing every 5 minutes reduces write frequency
- Partial wake lock used (doesn't turn on screen)

### Memory Management
- Sensor data buffered in memory between saves
- ~500-600 sensor readings per 5-minute cycle
- Data cleared after successful Firestore save
- Reasonable for 8-10 hour sleep sessions

### Network Efficiency
- Data sent to Firestore every 5 minutes (not real-time)
- Batch inserts reduce network overhead
- Offline support via Firebase SDK (queues writes)
- Minimal bandwidth for sensor data

### Database Indexing

Create these Firestore indexes for optimal performance:
```
Collection: sleep_records
Composite Index:
  - userId (Ascending)
  - recordDate (Descending)
```

---

## Project Configuration Notes

- **ProGuard:** Release builds use ProGuard for code obfuscation (`proguard-rules.pro`)
- **Version:** Currently at 1.0 (versionCode: 1, versionName: "1.0")
- **Git:** Project is version-controlled with `.gitignore` configured
- **Local Properties:** `local.properties` contains local SDK/NDK paths (not committed to Git)
- **Gradle Wrapper:** Uses Gradle 8.x wrapper for consistent builds
- **Dependencies:** Centralized version management via `gradle/libs.versions.toml`

---

## Related Documentation

For detailed component information, see **SLEEPTRACKER.md** which includes:
- Complete MVVM architecture diagrams
- Data flow diagrams for all major flows
- Detailed method signatures and responsibilities
- Example JSON data structures
- Error handling strategies
- Future enhancement roadmap

---

*Last Updated: January 2, 2026*
*Documentation Status: Current & Complete*

