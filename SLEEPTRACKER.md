# Sleep Tracker Overview - Complete Guide

## Project Overview

The Sleep Tracker is an Android application designed to help users monitor and record their sleep patterns. It combines manual sleep logging with sensor-based automatic tracking using device accelerometer and light sensors. The app stores all data in Firebase Firestore for persistent backup and cross-device synchronization.

**Key Technologies:**
- **Language:** Java
- **Architecture:** MVVM (Model-View-ViewModel) with Repository Pattern
- **Database:** Firebase Firestore
- **Backend Services:** Foreground Service for continuous sensor monitoring
- **Target SDK:** Android 36 | **Minimum SDK:** 24

---

## Core Features

### 1. **Manual Sleep Logging**
Users can manually record their sleep sessions by:
- Selecting start date/time and end date/time using date and time pickers
- Rating sleep quality on a 1-5 scale
- Adding optional notes about the sleep session
- System automatically calculates sleep duration between times

**Key Components:**
- `SleepFragment` - UI for manual sleep entry
- `SleepRecord` - Data model for manual sleep logs

### 2. **Sensor-Based Sleep Tracking**
Automatic monitoring of sleep using device sensors:
- **Accelerometer:** Detects movement patterns throughout the night
- **Light Sensor:** Monitors ambient light levels in the sleep environment
- Runs continuously in the background using a foreground service

**Key Components:**
- `SleepSensorService` - Background service handling sensor data collection
- `SleepSensorData` - Data model storing sensor readings and analysis

### 3. **Sleep Records Management**
Complete CRUD operations for sleep records:
- Create new sleep records manually or from sensor data
- View list of all recorded sleep sessions
- Edit existing sleep records
- Delete unwanted records
- Sort and filter by date

**Key Components:**
- `SleepViewModel` - View model managing sleep data lifecycle
- `SleepRepository` - Data access layer for Firestore operations
- `SleepRecordAdapter` - RecyclerView adapter for displaying sleep records

---

## Architecture & Design Patterns

### MVVM Architecture

```
┌─────────────────────────────────────────────┐
│           UI Layer (View)                   │
│  ┌──────────────────────────────────────┐   │
│  │      SleepFragment                   │   │
│  │  - Date/Time Pickers                 │   │
│  │  - Sleep Quality Spinner             │   │
│  │  - Notes Input                       │   │
│  │  - RecyclerView (Sleep Records)      │   │
│  │  - Sensor Control Buttons            │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                    ▲
                    │ (LiveData Observers)
                    ▼
┌─────────────────────────────────────────────┐
│         ViewModel Layer                     │
│  ┌──────────────────────────────────────┐   │
│  │      SleepViewModel                  │   │
│  │  - sleepRecords: MutableLiveData     │   │
│  │  - isLoading: MutableLiveData        │   │
│  │  - errorMessage: MutableLiveData     │   │
│  │  - selectedRecord: MutableLiveData   │   │
│  │  - loadSleepRecords()                │   │
│  │  - addSleepRecord()                  │   │
│  │  - updateSleepRecord()               │   │
│  │  - deleteSleepRecord()               │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                    ▲
                    │ (Repository calls)
                    ▼
┌─────────────────────────────────────────────┐
│       Repository & Service Layer            │
│  ┌──────────────────────────────────────┐   │
│  │      SleepRepository                 │   │
│  │  - addSleepRecord()                  │   │
│  │  - getSleepRecordsByUser()           │   │
│  │  - updateSleepRecord()               │   │
│  │  - deleteSleepRecord()               │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │      SleepSensorService              │   │
│  │  - startSleepMonitoring()            │   │
│  │  - stopSleepMonitoring()             │   │
│  │  - Sensor data collection            │   │
│  │  - Periodic data saving              │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
                    ▲
                    │ (Firestore queries)
                    ▼
┌─────────────────────────────────────────────┐
│       Data Models                           │
│  ┌──────────────────────────────────────┐   │
│  │      SleepRecord                     │   │
│  │  - id: String                        │   │
│  │  - userId: String                    │   │
│  │  - sleepStartTime: Date              │   │
│  │  - sleepEndTime: Date                │   │
│  │  - sleepDuration: int (minutes)      │   │
│  │  - sleepQuality: int (1-5)           │   │
│  │  - notes: String                     │   │
│  │  - recordDate: Date                  │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │      SleepSensorData                 │   │
│  │  - id: String                        │   │
│  │  - userId: String                    │   │
│  │  - sessionStartTime: Date            │   │
│  │  - sessionEndTime: Date              │   │
│  │  - movementData: List<MovementData>  │   │
│  │  - lightData: List<LightData>        │   │
│  │  - estimatedSleepQuality: int        │   │
│  │  - averageMovementPerHour: float     │   │
│  │  - averageLightLevel: float          │   │
│  │  - wasDarkEnvironment: boolean       │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## Component Descriptions

### Frontend Components

#### **SleepFragment.java**
Main UI fragment for sleep tracking functionality.

**Responsibilities:**
- Display date/time pickers for sleep session times
- Show sleep quality spinner (1-5 scale)
- Provide notes input field
- Display list of recorded sleep sessions
- Manage sensor tracking start/stop buttons
- Handle user interactions and validations

**Key Methods:**
- `onCreateView()` - Inflate layout and initialize binding
- `onViewCreated()` - Setup UI components, observers, and listeners
- `initializeDateTimePickers()` - Initialize calendars with default values
- `setupRecyclerView()` - Configure list display of sleep records
- `setupListeners()` - Attach click listeners to buttons
- `setupObservers()` - Observe LiveData changes from ViewModel
- `setupSensorTracking()` - Setup sensor monitoring buttons
- `saveSleepRecord()` - Validate and save manual sleep entry
- `showDatePicker()` / `showTimePicker()` - Display date/time selection dialogs
- `updateDateTimeButtons()` - Refresh displayed times and calculate duration
- `startSensorTracking()` - Initiate sensor monitoring service
- `stopSensorTracking()` - Halt sensor monitoring
- `checkPermissions()` - Verify required permissions granted
- `requestPermissions()` - Request sensor permissions from user

**UI Elements:**
```
┌────────────────────────────────────┐
│   START DATE/TIME SELECTION        │
│  [Start Date: Sat, Jan 01, 2025]  │
│  [Start Time: 22:00]               │
│                                    │
│   END DATE/TIME SELECTION          │
│  [End Date: Sun, Jan 02, 2025]    │
│  [End Time: 07:00]                 │
│                                    │
│   Sleep Quality: [▼ Quality]       │
│                                    │
│   Notes:                           │
│  [_______________________]         │
│                                    │
│  [Save Sleep Record (9h 0m)]       │
│                                    │
├────────────────────────────────────┤
│   SENSOR TRACKING                  │
│  🟢 Sensor tracking active         │
│  [Stop Sensor Tracking]            │
├────────────────────────────────────┤
│   SLEEP RECORDS LIST               │
│  ┌──────────────────────────────┐  │
│  │ Date | Duration | Quality    │  │
│  │ Record 1 | Edit | Delete    │  │
│  │ Record 2 | Edit | Delete    │  │
│  │ Record 3 | Edit | Delete    │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

**Permissions Required:**
- `android.permission.ACTIVITY_RECOGNITION` - For movement detection
- `android.permission.WAKE_LOCK` - To keep CPU active during monitoring

**Important Constants:**
```java
PERMISSION_REQUEST_CODE = 1001
REQUIRED_PERMISSIONS = {
    Manifest.permission.ACTIVITY_RECOGNITION,
    Manifest.permission.WAKE_LOCK
}
```

#### **SleepRecordAdapter.java**
RecyclerView adapter for displaying sleep records in a list.

**Responsibilities:**
- Bind sleep record data to list item views
- Handle item click events for editing
- Handle delete actions
- Display formatted sleep data (date, duration, quality)

**Callback Interfaces:**
- `OnItemClickListener` - Triggered when user clicks a record
- `OnDeleteClickListener` - Triggered when user clicks delete

---

### ViewModel

#### **SleepViewModel.java**
Lifecycle-aware component managing UI data and business logic.

**Responsibilities:**
- Manage sleep records state across configuration changes
- Provide reactive data streams via LiveData
- Coordinate between UI and repository layer
- Handle loading and error states

**LiveData Streams:**
```java
sleepRecords: MutableLiveData<List<SleepRecord>>  // List of user's sleep records
isLoading: MutableLiveData<Boolean>                 // Loading state indicator
errorMessage: MutableLiveData<String>              // Error messages for UI
selectedRecord: MutableLiveData<SleepRecord>       // Currently selected record
```

**Key Methods:**
- `loadSleepRecords(userId)` - Fetch all records for a user
- `addSleepRecord(record, userId)` - Create new sleep record
- `updateSleepRecord(recordId, record)` - Modify existing record
- `deleteSleepRecord(recordId, userId)` - Remove record
- `selectRecord(record)` - Mark record as selected
- `clearSelectedRecord()` - Deselect current record

**Data Flow:**
```
SleepFragment
     │
     ├─ observes ─→ LiveData (sleepRecords, isLoading, errorMessage)
     │
     └─ calls ────→ SleepViewModel methods
                         │
                         └─ calls ────→ SleepRepository methods
                                            │
                                            └─ Firestore queries
```

---

### Data Models

#### **SleepRecord.java**
Represents a manually logged sleep session.

**Fields:**
```java
id: String                    // Unique Firestore document ID
userId: String               // User who logged the sleep
sleepStartTime: Date         // When sleep began
sleepEndTime: Date           // When sleep ended
sleepDuration: int           // Duration in minutes (auto-calculated)
sleepQuality: int            // Quality rating 1-5 (5 = best)
notes: String                // User notes about sleep quality
recordDate: Date             // Timestamp when record was created
```

**Auto-Calculations:**
- `sleepDuration` is automatically calculated from the difference between `sleepEndTime` and `sleepStartTime` (in minutes)
- `recordDate` is automatically set to current date/time on creation

**Firestore Compatibility:**
- Includes no-argument constructor required by Firestore
- Can be automatically serialized/deserialized by Firestore

**Example Sleep Record:**
```json
{
  "id": "doc_12345",
  "userId": "user_001",
  "sleepStartTime": "2025-01-01T22:00:00Z",
  "sleepEndTime": "2025-01-02T07:00:00Z",
  "sleepDuration": 540,
  "sleepQuality": 4,
  "notes": "Good sleep, minimal interruptions",
  "recordDate": "2025-01-02T07:15:00Z"
}
```

#### **SleepSensorData.java**
Contains sensor-based sleep analysis data.

**Fields:**
```java
id: String                           // Unique record ID
userId: String                       // User associated with session
sessionStartTime: Date              // When sensor monitoring started
sessionEndTime: Date                // When sensor monitoring stopped
movementData: List<MovementData>    // Accelerometer readings throughout session
lightData: List<LightData>          // Light sensor readings throughout session
estimatedSleepQuality: int          // AI-estimated quality based on patterns (1-5)
averageMovementPerHour: float       // Average movement intensity per hour
averageLightLevel: float            // Average light exposure during session (lux)
wasDarkEnvironment: boolean         // Whether environment was dark (< LIGHT_DARK_THRESHOLD)
```

**Related Models:**

**MovementData:**
```java
timestamp: Date              // When movement was detected
acceleration: float          // Magnitude of acceleration (m/s²)
movementIntensity: int      // Categorized intensity level (1-5)
```

**LightData:**
```java
timestamp: Date              // When light was measured
lightLevel: float           // Ambient light in lux
isDark: boolean             // Is environment dark
```

**Example Sensor Data:**
```json
{
  "id": "sensor_doc_456",
  "userId": "user_001",
  "sessionStartTime": "2025-01-01T22:00:00Z",
  "sessionEndTime": "2025-01-02T07:00:00Z",
  "movementData": [
    { "timestamp": "2025-01-01T22:15:00Z", "acceleration": 0.5, "movementIntensity": 1 },
    { "timestamp": "2025-01-01T22:30:00Z", "acceleration": 0.3, "movementIntensity": 1 },
    ...
  ],
  "lightData": [
    { "timestamp": "2025-01-01T22:00:00Z", "lightLevel": 5.2, "isDark": true },
    { "timestamp": "2025-01-01T22:30:00Z", "lightLevel": 4.8, "isDark": true },
    ...
  ],
  "estimatedSleepQuality": 4,
  "averageMovementPerHour": 2.1,
  "averageLightLevel": 4.9,
  "wasDarkEnvironment": true
}
```

---

### Repository Layer

#### **SleepRepository.java**
Data access object handling Firestore operations.

**Responsibilities:**
- Abstract database operations from business logic
- Manage Firestore collection references
- Provide callback-based async operations

**Firestore Collection:** `sleep_records`

**Key Methods:**

```java
void addSleepRecord(SleepRecord record, OnCompleteListener listener)
```
- Creates new sleep record in Firestore
- Auto-generates document ID
- Calls listener with DocumentReference on completion

```java
void getSleepRecordsByUser(String userId, OnCompleteListener listener)
```
- Retrieves all sleep records for specific user
- Filters by `userId` field
- Orders results by `recordDate` descending (newest first)
- Calls listener with QuerySnapshot on completion

```java
void updateSleepRecord(String recordId, SleepRecord record, OnCompleteListener listener)
```
- Modifies existing sleep record
- Replaces entire document with provided data
- Calls listener with Void on completion

```java
void deleteSleepRecord(String recordId, OnCompleteListener listener)
```
- Removes record from Firestore
- Uses document ID to target record
- Calls listener with Void on completion

```java
void getSleepRecord(String recordId, OnCompleteListener listener)
```
- Fetches single sleep record by ID
- Calls listener with DocumentSnapshot on completion

**Firestore Query Examples:**
```firestore
// Get all records for user
db.collection("sleep_records")
  .whereEqualTo("userId", "user_001")
  .orderBy("recordDate", descending)
  .get()

// Add new record
db.collection("sleep_records")
  .add({ userId, sleepStartTime, sleepEndTime, ... })

// Update record
db.collection("sleep_records")
  .document("doc_id")
  .set({ updated fields })

// Delete record
db.collection("sleep_records")
  .document("doc_id")
  .delete()
```

---

### Service Layer

#### **SleepSensorService.java**
Android Foreground Service for continuous sensor monitoring.

**Purpose:**
Run background sensor data collection and analysis even when the app is not actively being viewed.

**Sensor Types:**

1. **Accelerometer** (`TYPE_ACCELEROMETER`)
   - Measures device acceleration in 3 axes (x, y, z)
   - Used to detect movement and position changes
   - Helps identify sleep quality and disturbances
   - Sampling interval: 5 seconds
   - Threshold: 1.5 m/s²

2. **Light Sensor** (`TYPE_LIGHT`)
   - Measures ambient light in lux
   - Determines if sleep environment is dark
   - Helps assess sleep conditions
   - Sampling interval: 30 seconds
   - Threshold: 10.0 lux (dark/light boundary)

**Lifecycle:**

```
Service Created
    │
    ├─ Create Notification Channel
    ├─ Initialize SensorManager
    ├─ Initialize PowerManager (WakeLock)
    └─ Initialize ScheduledExecutor

Service Start Command
    │
    ├─ Extract action from Intent ("start" or "stop")
    └─ Extract userId from Intent

If Action = "start":
    │
    ├─ Start Foreground Service with notification
    ├─ Acquire WakeLock (keep CPU awake)
    ├─ Create new SleepSensorData session
    ├─ Register sensor listeners
    ├─ Schedule periodic save task (every 5 minutes)
    └─ Set isMonitoring = true

If Action = "stop":
    │
    ├─ Unregister sensor listeners
    ├─ Release WakeLock
    ├─ Save final session data
    ├─ Stop foreground service
    └─ Set isMonitoring = false

Service Destroyed
    │
    └─ Cleanup resources
```

**Key Methods:**

```java
void onCreate()
```
- Initializes all sensors and system resources
- Creates notification channel for Android 8.0+
- Creates WakeLock to prevent CPU sleep
- Creates scheduled executor for periodic tasks

```java
int onStartCommand(Intent intent, int flags, int startId)
```
- Receives start/stop actions
- Routes to appropriate handling method
- Returns `START_STICKY` for auto-restart if killed

```java
void startSleepMonitoring()
```
- Starts foreground service notification
- Acquires WakeLock
- Registers sensor listeners with `SENSOR_DELAY_NORMAL`
- Initializes sensor data buffer
- Schedules periodic save task

```java
void stopSleepMonitoring()
```
- Unregisters all sensor listeners
- Releases WakeLock
- Saves accumulated sensor data to Firestore
- Stops foreground service

```java
void onSensorChanged(SensorEvent event)
```
- Called when sensor readings change
- Throttles high-frequency accelerometer data
- Buffers readings for batch processing
- Detects movement and light changes

```java
void checkAndSaveData()
```
- Periodic task running every 5 minutes
- Analyzes buffered sensor data
- Calculates movement averages and light statistics
- Saves intermediate results to Firestore

**Notification:**
```
┌──────────────────────────────────────┐
│  Sleep Tracker Monitoring            │
│  Sensor tracking active              │
│  [Stop]                              │
└──────────────────────────────────────┘
```

**WakeLock:**
- Type: `PARTIAL_WAKE_LOCK`
- Purpose: Keep CPU running for continuous monitoring
- Prevents device from entering deep sleep
- Released when monitoring stops to save battery

**Sensor Configuration:**
```java
MOVEMENT_THRESHOLD = 1.5f          // m/s²
LIGHT_DARK_THRESHOLD = 10.0f       // Lux
ACCELEROMETER_INTERVAL = 5000      // 5 seconds
LIGHT_SENSOR_INTERVAL = 30000      // 30 seconds
SAVE_INTERVAL = 300000             // 5 minutes
```

**Thread Management:**
- Uses `ScheduledExecutorService` for periodic background tasks
- Keeps service on separate thread to avoid ANR (Application Not Responding)
- Single-threaded executor to prevent race conditions

---

## Data Flow Diagrams

### Manual Sleep Logging Flow

```
User Action: Input sleep times
             │
             ▼
SleepFragment.saveSleepRecord()
             │
             ├─ Validate end time > start time
             ├─ Get sleepQuality from spinner
             ├─ Get notes from EditText
             │
             ▼
Create SleepRecord object
  - Calculate sleepDuration
  - Set userId
  - Set recordDate
             │
             ▼
SleepViewModel.addSleepRecord()
             │
             ▼
SleepRepository.addSleepRecord()
             │
             ▼
Firebase Firestore.add()
             │
             ▼
Callback: success?
  ├─ YES: Call loadSleepRecords()
  └─ NO: Show error message
             │
             ▼
Update LiveData
             │
             ▼
RecyclerView updates
(SleepRecordAdapter.submitList())
```

### Sensor Monitoring Flow

```
User Click: "Start Sensor Tracking"
             │
             ▼
SleepFragment.startSensorTracking()
             │
             ├─ Check permissions
             │  ├─ All granted? YES → Continue
             │  └─ NO → Request permissions
             │
             ▼
Create Intent for SleepSensorService
  - action: "start"
  - user_id: userId
             │
             ▼
startForegroundService() / startService()
             │
             ▼
SleepSensorService.onStartCommand()
             │
             ▼
SleepSensorService.startSleepMonitoring()
             │
             ├─ Create SleepSensorData session
             ├─ Acquire WakeLock
             ├─ Register sensor listeners
             └─ Schedule save task (every 5 min)
             │
             ▼
Sensor Data Collection Loop
  │
  ├─ Accelerometer: 5-second intervals
  │   ├─ Detect movement > MOVEMENT_THRESHOLD
  │   └─ Buffer MovementData
  │
  ├─ Light Sensor: 30-second intervals
  │   ├─ Measure light level (lux)
  │   └─ Buffer LightData
  │
  └─ Every 5 minutes: Save to Firestore
       ├─ Calculate averages
       ├─ Estimate sleep quality
       └─ Store SleepSensorData

When User Stops:
User Click: "Stop Sensor Tracking"
             │
             ▼
SleepFragment.stopSensorTracking()
             │
             ▼
Intent: action="stop"
             │
             ▼
SleepSensorService.onStartCommand()
             │
             ▼
SleepSensorService.stopSleepMonitoring()
             │
             ├─ Unregister sensors
             ├─ Release WakeLock
             ├─ Save final session
             └─ Stop foreground
             │
             ▼
SleepSensorData saved to Firestore
```

### Data Retrieval Flow

```
App Startup
     │
     ▼
SleepFragment.onViewCreated()
     │
     ├─ Get userId from SessionManager
     │
     ▼
SleepViewModel.loadSleepRecords(userId)
     │
     ▼
SleepRepository.getSleepRecordsByUser()
     │
     ▼
Firestore Query:
  collection("sleep_records")
    .whereEqualTo("userId", userId)
    .orderBy("recordDate", DESCENDING)
    .get()
     │
     ▼
Live data updates
     │
     ├─ sleepRecords: List<SleepRecord> ← QuerySnapshot
     └─ isLoading: false
     │
     ▼
RecyclerView observes LiveData change
     │
     ▼
SleepRecordAdapter.submitList(records)
     │
     ▼
RecyclerView renders list items
```

---

## Permission Model

### Required Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

**ACTIVITY_RECOGNITION** (Android 10+, Runtime)
- Allows access to sensor data like accelerometer
- Must be granted at runtime on Android 10+
- Used for movement detection during sleep

**WAKE_LOCK** (Install-time)
- Allows preventing CPU from sleeping
- Needed to keep sensors active during monitoring
- Declared in manifest, no runtime prompt needed

### Permission Flow

```
SleepFragment.setupSensorTracking()
     │
     └─ User clicks "Start Sensor Tracking"
             │
             ▼
        checkPermissions()
             │
             ├─ Check ACTIVITY_RECOGNITION
             ├─ Check WAKE_LOCK
             │
             ├─ All granted? YES
             │  │
             │  └─ startSensorTracking()
             │
             └─ Some missing? NO
                  │
                  └─ requestPermissions()
                       │
                       ▼
                  System permission dialog
                       │
                       ▼
                  User response
                       │
                       ├─ Grant ALL → startSensorTracking()
                       └─ Grant SOME → Show error toast
```

---

## Database Schema (Firestore)

### Collection: `sleep_records`

**Document Structure:**
```
sleep_records/
├── doc_001/
│   ├── id: "doc_001"
│   ├── userId: "user_001"
│   ├── sleepStartTime: Timestamp
│   ├── sleepEndTime: Timestamp
│   ├── sleepDuration: 540
│   ├── sleepQuality: 4
│   ├── notes: "Had a good night"
│   └── recordDate: Timestamp
│
├── doc_002/
│   ├── id: "doc_002"
│   ├── userId: "user_001"
│   ├── sleepStartTime: Timestamp
│   ├── sleepEndTime: Timestamp
│   ├── sleepDuration: 480
│   ├── sleepQuality: 3
│   ├── notes: "Woke up multiple times"
│   └── recordDate: Timestamp
│
└── doc_003/
    ├── id: "doc_003"
    ├── userId: "user_002"
    ├── sleepStartTime: Timestamp
    ├── sleepEndTime: Timestamp
    ├── sleepDuration: 600
    ├── sleepQuality: 5
    ├── notes: "Excellent sleep"
    └── recordDate: Timestamp
```

### Collection: `sleep_sensor_data` (Optional)

**Document Structure:**
```
sleep_sensor_data/
├── sensor_001/
│   ├── id: "sensor_001"
│   ├── userId: "user_001"
│   ├── sessionStartTime: Timestamp
│   ├── sessionEndTime: Timestamp
│   ├── movementData: [
│   │   {
│   │     timestamp: Timestamp,
│   │     acceleration: 0.5,
│   │     movementIntensity: 1
│   │   },
│   │   { ... }
│   │ ]
│   ├── lightData: [
│   │   {
│   │     timestamp: Timestamp,
│   │     lightLevel: 5.2,
│   │     isDark: true
│   │   },
│   │   { ... }
│   │ ]
│   ├── estimatedSleepQuality: 4
│   ├── averageMovementPerHour: 2.1
│   ├── averageLightLevel: 4.9
│   └── wasDarkEnvironment: true
│
└── sensor_002/
    └── ...
```

### Indexes Needed

For optimal query performance, create these Firestore indexes:

```
Collection: sleep_records
  - Composite Index:
    Fields:
      - userId (Ascending)
      - recordDate (Descending)
```

---

## Key Features & Functionality

### Manual Sleep Entry

**Features:**
- Date/Time picker for flexible entry
- Default times: Yesterday 22:00 → Today 07:00
- Overnight sleep support (crosses midnight)
- Sleep quality rating (1-5 scale with descriptors)
- Optional notes field
- Real-time duration calculation
- Validation to prevent invalid time ranges

**Validation Rules:**
```java
if (endTime <= startTime) {
    showError("End date/time must be after start date/time")
}
```

### Sensor Monitoring

**Features:**
- Automatic background monitoring
- Real-time movement detection
- Light level measurement
- Battery optimized (throttled sampling)
- Persistent across app restarts
- Automatic data save every 5 minutes
- Foreground notification for user awareness
- One-tap start/stop control

**Smart Features:**
- Detects dark environment for optimal sleep conditions
- Calculates movement patterns to estimate sleep quality
- Analyzes light exposure for sleep hygiene insights
- Buffers data for efficient processing

### Sleep Records Management

**Features:**
- List all recorded sleep sessions
- Sort by most recent first
- Show duration and quality at a glance
- Edit individual records (planned)
- Delete records with confirmation
- Empty state messaging
- Loading state indication
- Error handling with user messages

---

## Error Handling

### User-Facing Errors

1. **Invalid Time Range**
   ```
   "End date/time must be after start date/time"
   ```

2. **User Not Logged In**
   ```
   "User not logged in"
   "Please log in first"
   ```

3. **Permission Denied**
   ```
   "Permissions required for sensor tracking"
   ```

4. **Sensor Not Available**
   ```
   "Accelerometer not available on this device"
   ```

5. **Service Start Failed**
   ```
   "Failed to start sensor tracking: {error message}"
   "Failed to stop sensor tracking: {error message}"
   ```

6. **Database Errors**
   ```
   "Failed to load sleep records: {error}"
   "Failed to add sleep record: {error}"
   "Failed to update sleep record: {error}"
   "Failed to delete sleep record: {error}"
   ```

### Developer Logging

The service uses Android Log with `TAG = "SleepSensorService"` for debugging:
```java
Log.d(TAG, "Starting sleep monitoring");
Log.w(TAG, "Already monitoring");
Log.e(TAG, "Accelerometer not available");
```

---

## User Experience Flow

### Typical Usage Scenarios

**Scenario 1: Manual Sleep Entry**
```
1. User opens Sleep tab
2. See yesterday's 22:00 → today's 07:00 pre-filled
3. Adjust times if needed using pickers
4. Select sleep quality from spinner
5. Add optional notes
6. Tap "Save Sleep Record"
7. Get success confirmation
8. Record appears in list
```

**Scenario 2: Automatic Sensor Tracking**
```
1. User prepares for bed
2. Opens Sleep tab and taps "Start Sensor Tracking"
3. Grants permissions if first time
4. Sees "🟢 Sensor tracking active" indicator
5. Places phone on bedside table
6. Phone monitors movement and light all night
7. Next morning, taps "Stop Sensor Tracking"
8. Sensor data automatically saved
9. Combined view shows both manual and sensor data
```

**Scenario 3: Reviewing Sleep History**
```
1. User opens Sleep tab
2. Sees list of recent sleep records
3. Each record shows date, duration, quality
4. Can scroll through history
5. Can delete records they don't want
6. Can edit records (coming soon)
```

---

## Performance Considerations

### Battery Usage
- Foreground service keeps device active (requires WakeLock)
- Sensor sampling throttled (5s/30s intervals, not real-time)
- Batch processing every 5 minutes
- Uses partial wake lock (doesn't turn on screen)

### Memory Usage
- Buffers sensor data in memory between saves
- ~500-600 sensor readings per 5-minute cycle
- Reasonable for 8-10 hour sleep session
- Data saved to disk before loading new session

### Network Usage
- Data sent to Firestore every 5 minutes during monitoring
- Synchronous manual record saves
- Periodic bulk updates for sensor analysis
- Handles offline gracefully (Firebase queues writes)

---

## Future Enhancements

### Planned Features
- [ ] Sleep record editing functionality
- [ ] Advanced sleep analytics (trends, statistics)
- [ ] Sleep recommendations based on patterns
- [ ] Heart rate sensor integration
- [ ] Export sleep data (CSV/PDF)
- [ ] Sleep goals and reminders
- [ ] Integration with wearables
- [ ] Machine learning for sleep quality prediction
- [ ] Sleep diary with mood tracking
- [ ] Shareable sleep reports

### Potential Improvements
- [ ] Offline-first architecture (local SQLite backup)
- [ ] Push notifications for sleep reminders
- [ ] Data synchronization across devices
- [ ] Advanced visualization (charts, graphs)
- [ ] Voice notes for sleep sessions
- [ ] Sleep schedule optimization suggestions

---

## Testing Considerations

### Unit Tests
- Test SleepViewModel methods
- Test SleepRepository Firestore queries
- Test SleepRecord duration calculations
- Test time validation logic

### Integration Tests
- Test sensor data collection
- Test Firestore write/read operations
- Test permission flows
- Test service lifecycle

### UI Tests
- Test date/time picker interactions
- Test form validation
- Test RecyclerView rendering
- Test sensor button state changes

---

## Troubleshooting Guide

### Sensor Tracking Not Working
1. Check permissions are granted (Settings > Apps > Sleep Tracker > Permissions)
2. Verify device has accelerometer (most Android devices do)
3. Check if "Optimize battery usage" is limiting the app
4. Restart the app and try again

### Data Not Saving
1. Check internet connection
2. Verify Firestore database is accessible
3. Check user is logged in (SessionManager)
4. Look at logcat for specific Firestore errors

### Permissions Not Requested
1. Clear app cache (Settings > Apps > Sleep Tracker > Storage)
2. Clear app data (will lose all data!)
3. Reinstall app
4. Accept all permissions when prompted

### Battery Draining Quickly
1. Reduce sensor monitoring time
2. Disable light sensor if not needed
3. Increase sampling intervals
4. Use on devices with efficient sensors

---

## References & Documentation

- **Android Documentation:** https://developer.android.com/guide
- **Firebase Firestore:** https://firebase.google.com/docs/firestore
- **Sensor Framework:** https://developer.android.com/guide/topics/sensors/sensors_overview
- **Services:** https://developer.android.com/guide/components/services
- **MVVM Architecture:** https://developer.android.com/jetpack/guide
- **LiveData:** https://developer.android.com/topic/libraries/architecture/livedata

---

**Document Version:** 1.0  
**Last Updated:** January 2, 2026  
**Status:** Current

