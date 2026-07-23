# ETO (Electronic Token Online) Hospital Queue Management App

ETO is a high-fidelity, native Android mobile application designed to replace traditional paper tokens in hospitals and clinics with an automated, real-time digital queue system. 

It is built from the ground up using **Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite Database**, and **Kotlin Coroutines/StateFlow** following **Clean Architecture** patterns.

---

## 🎨 Visual Identity & Premium Aesthetics

ETO is optimized with a sleek, dark-theme visual design system that aligns with premium hospital environments:
*   **Midnight Dark Background (`#01082D`)**: Depth-inducing primary background.
*   **Navy Deep (`#041D56`)**: Core cards, dashboard elements, and surfaces.
*   **Steel Blue (`#266CA9`)**: Borders, selected tags, and interactive outlines.
*   **Ice Cyan (`#ADE1FB`)**: Highlights, sweeping circular gradients, and active trackers.

---

## ✨ Premium Physics-Based Animations (ReactBits Ports)

ETO integrates custom-ported high-fidelity gesture and rendering effects originally inspired by ReactBits:
1.  **`bounceClick` Modifier**: Adds custom spring-physics scale feedback to button clicks.
2.  **`magnetEffect` Modifier**: Attracts components to the user's drag gestures, snapping back with spring physics.
3.  **`shimmer` Modifier**: An animated shader gradient simulating skeleton loaders.
4.  **`ShinyText`**: A shiny metallic reflection moving continuously across text headers.
5.  **`SpotlightCard`**: Radial touch-coordinate lighting following the user's fingers dynamically.
6.  **Concentric Live Queue Pulse**: A custom Canvas element that draws fading and scaling pulse rings for serving tokens.
7.  **Interactive Spline Line Chart**: An admin analytics line chart drawn on custom Canvas using cubic Bezier points, supporting entrance animations, node snapping, and touch tooltip indicators.

---

## 👥 Roles & Workflows

ETO features an app shell with a **Role Switcher** that allows testing all 4 key healthcare personas on a single device:

*   **Patient Portal**:
    *   Search and filter doctors by specialty, availability, and rating.
    *   Enter chief complaints/symptoms to request online consultation tokens.
    *   Monitor queue progression through an interactive circular live tracker.
    *   View consultation history, diagnosis, prescriptions, and billing receipts.
*   **Receptionist Dashboard**:
    *   Approve or reject incoming online token requests.
    *   Register walk-in patients directly into any doctor's waiting queue.
    *   Control doctor active queues (Call Next / Skip Patient).
    *   Collect payments for completed consultations.
*   **Doctor Consulting Desk**:
    *   Select active profile to load personal queue.
    *   Open the Consultation Workspace for the patient currently in the room.
    *   Input diagnosis, type prescriptions, and set consultation fees to finalize visits.
*   **Admin Dashboard**:
    *   Analyze daily hospital analytics (peak load spline chart, total collections, skip rates).
    *   Toggle the background simulation ticker.
    *   Clear consultation data to reset simulator logs.

---

## 🏗️ Clean Architecture & Tech Stack

The application is structured into clearly defined architectural layers:

*   **Presentation Layer**:
    *   `MainActivity.kt` & Navigation Host.
    *   Custom Compose Views: `PatientView`, `ReceptionistView`, `DoctorView`, `AdminView`.
    *   Aesthetic Modifiers & Components (`Components.kt`).
    *   Theme definition (`Theme.kt`, `Color.kt`, `Type.kt`).
    *   `EtoViewModel.kt`: Implements reactive MVVM logic, manages active session data, and schedules simulation tasks.
*   **Domain Layer**:
    *   `Models.kt`: Business models (`Doctor`, `Token`, `Department`, etc.).
    *   `EtoRepository.kt`: Clean interface decoupling business rules from Room database implementations.
*   **Data Layer**:
    *   `EtoRepositoryImpl.kt`: Implements repository actions, including automated seeding of mock doctors and departments.
    *   `AppDatabase.kt`: SQLite Room database setup.
    *   `Entities.kt`: SQLite schema representations.
    *   `EtoDao.kt`: Room query structures.

---

## 🛠️ Build & Installation

### Requirements
*   **Android Studio** (Koala / Ladybug or newer recommended).
*   **JDK 17 or 21** (Android Studio's internal bundled JDK is recommended).
*   **Android SDK 34** (compileSdk & targetSdk).

### How to Run in Android Studio
1.  Clone this repository:
    ```bash
    git clone https://github.com/KarthickRaghul/ETO---E-Token-System.git
    ```
2.  Open **Android Studio** and choose **Open Project**.
3.  Navigate to and select the cloned root directory.
4.  Wait for Android Studio to sync with the Kotlin Gradle files and download dependencies.
5.  Connect an Android device (or launch a virtual device/emulator).
6.  Click **Run** (`Shift + F10`) to build and launch the application.

---

## 👨‍💻 Verification & Stability

The project contains zero compilation errors or warnings. Build processes have been fully verified:
*   **Kotlin Compiler**: Build successfully passes debug Kotlin compilation tasks (`compileDebugKotlin`).
*   **Pointer Gesture Safety**: Pointer inputs on `SpotlightCard` use null-safe checks (`firstOrNull()`) to prevent gesture-tracking index crashes.
*   **Responsive Canvas Rendering**: Spline charts and progress indicators monitor bounds using `onSizeChanged` callbacks, ensuring proper layout on different screen resolutions.
