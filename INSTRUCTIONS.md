# ETO Android Application - Setup & Running Instructions

This document provides step-by-step instructions to import, build, run, and verify the ETO application in Android Studio.

---

## 📋 Prerequisites

Before opening the project, ensure you have the following installed on your developer machine:
- **JDK 17 or higher**
- **Android Studio Koala (2024.1.1) / Ladybug (2024.2.1) or newer**
- **Android SDK Platform API 34 (Android 14) or newer**

---

## 🛠️ Opening the Project in Android Studio

1. Launch **Android Studio**.
2. Click **Open** (or **File > Open**) and select the workspace folder: `/home/karthi/Projects/Eto`.
3. Android Studio will automatically identify the Gradle configuration files and start the initial sync.
4. During synchronization, the IDE will:
   - Configure local properties based on your Android SDK paths.
   - Seed dependencies, including Jetpack Compose Material 3, Room SQLite, Navigation, and Lifecycle ViewModels.
   - Generate local build artifacts.

---

## 🚀 Building & Launching the App

1. Once the Gradle sync completes, select your target device (an Android Emulator or connected physical device).
2. Click the **Run** button (green play icon) or press `Shift + F10`.
3. If running via terminal console, compile using the build task:
   ```bash
   ./gradlew assembleDebug
   ```
4. The debug APK will compile and install on your selected device.

---

## 🧪 Step-by-Step Interactive Test Walkthrough

Follow this sequence of steps to test the application's entire database integration, background simulations, and custom animations:

### Step 1: Patient Booking
1. On app launch, the default role is set to **PATIENT**.
2. Scroll to the "Available Doctors" section. Cards use the `SpotlightCard` modifier; try moving your mouse or finger over the card to observe the radial highlight effect.
3. Tap **Select Doctor** (note the physics-based `bounceClick` spring feedback).
4. Enter symptoms (e.g., "Mild headache and body aches") in the notes box.
5. Tap **Book Token** to send the request to the database.

### Step 2: Front Desk Approval
1. Switch your role to **RECEPTIONIST** using the dropdown at the top right of the toolbar.
2. Navigate to the **Requests** tab.
3. You will see the patient's token request listed. Tap **Approve** (the green check button). The token moves from `PENDING` to `APPROVED`.

### Step 3: Queue Live Tracking
1. Switch your role back to **PATIENT**.
2. A premium tracking card will appear at the top. Observe:
   - The double pulsing concentric rings (Canvas animated halo).
   - The custom gradient progress ring wrapping the patient's active token number.
3. Tap the floating notification bell in the bottom-right corner to open the **Virtual Phone Alerts** drawer. You will see SMS alerts tracking the status of your token.

### Step 4: Medical Diagnosis
1. Switch your role to **RECEPTIONIST** and tap the **Queues** tab.
2. Select **Call Next** to call the patient into the consulting room. The token status becomes `SERVING`.
3. Switch your role to **DOCTOR** in the top selector.
4. Select the corresponding doctor profile. The active patient workspace will open.
5. Enter a Diagnosis (e.g., "Acute Migraine") and Prescription (e.g., "Paracetamol 500mg"), then tap **Finalize & Complete Consultation**.

### Step 5: Cash Payment Billing
1. Switch your role to **RECEPTIONIST** and click the **Billing** tab.
2. The completed consultation bill of `₹500` will be visible.
3. Tap **Record Cash Paid** (which uses a spring magnet attraction modifier) to mark the transaction as PAID.

### Step 6: Admin Analytics
1. Switch your role to **ADMIN**.
2. Review the **Hospital Queue Analytics** dashboard.
3. Interact with the **Patient Flow Trend** spline chart. Tap different coordinates along the curve to snap dotted indicator lines and display floating tooltip capsules.
4. Toggle the **Simulated Queue Ticker** switch. When active, background coroutines will periodically register walk-ins and progress the queues automatically.
