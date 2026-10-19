# PlantCare Pro 🌱

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575.svg?style=for-the-badge)](https://m3.material.io/)
[![Room Database](https://img.shields.io/badge/Persistence-Room%20(SQLite)-FF8F00.svg?style=for-the-badge)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

**An intelligent, offline-first botanical companion and garden management Android application.**  
*Track your plants, diagnose health issues with photo evidence, generate printable A4 PDF guides, and sync across devices.*

[Key Features](#-key-features) • [Architecture](#-architecture--tech-stack) • [Getting Started](#-getting-started) • [Configuration](#-configuration) • [Contributing](#-contributing)

</div>

---

## 🌿 About The Project

**PlantCare Pro** is a modern Android application engineered to help plant enthusiasts, urban gardeners, and botanists cultivate thriving gardens. Built with **100% Kotlin** and **Jetpack Compose**, the app pairs an offline-first **Room database** with optional **Firebase cloud sync**, ensuring your garden records are always accessible anywhere, even with zero network connectivity.

Whether diagnosing yellowing leaves, exporting custom PDF care manuals for field work, or cataloging vegetables and herbs, PlantCare Pro delivers a refined, tactile user experience adhering strictly to **Material Design 3** guidelines.

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 🪴 **Botanical Catalog** | Organize plants across **Fruits**, **Vegetables**, **Flowers**, and **Herbs** with high-resolution photo galleries and summaries. |
| 🔍 **Instant Search & Filter** | Search through plant names, care notes, and categories in real time with immediate UI updates. |
| 🩺 **Health Diagnosis & Remedies** | Log specific plant ailments, track leaf symptoms with photo evidence, and access organic remedies. |
| 📄 **Native A4 PDF Generator** | Generate beautifully formatted, printable A4 PDF field guides with diagrams and care routines. |
| 📎 **PDF Document Attachment** | Attach external PDFs and reference manuals directly to individual plant profiles. |
| 🌐 **Bilingual (English & বাংলা)** | One-tap instant language switching between English and Bengali across the entire interface. |
| 💾 **Offline-First & Local Backups** | All records are stored locally via Room SQLite. Export or import full JSON backups containing photos & PDFs. |
| ☁️ **Cloud Sync & Authentication** | Optional Firebase authentication and Realtime Database synchronization for multi-device backups. |
| 🌗 **Dynamic Material 3 Theming** | Fully responsive dark and light modes styled with an emerald botanical color palette. |

---

## 📱 Application Screens

- **🏠 Dashboard Screen:** Quick stats overview, category filter chips, search bar, and recently accessed plants.
- **📋 All Plants Screen:** Comprehensive, filterable garden list with instant search and visual cards.
- **🔍 Plant Detail Screen:** Deep-dive view featuring care guides, sickness history, photo evidence, PDF generation, and sharing.
- **✏️ Plant Add & Edit Screen:** Intuitive form to register new plants, capture photos, and configure tailored care tips.
- **🔐 Cloud & Account Screen:** Manage Firebase credentials, user profiles, and manual cloud sync triggers.

---

## 🏗 Architecture & Tech Stack

The application is structured following Google's **Modern Android Architecture (MVVM)** and **Unidirectional Data Flow (UDF)**:

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│   Jetpack Compose Screens  •  Material 3 Components   │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow (UI State)
                            │ Events / User Actions
┌───────────────────────────▼────────────────────────────┐
│                    ViewModel Layer                     │
│    PlantViewModel  •  UserSettings  •  I18nManager     │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutines / Flow
┌───────────────────────────▼────────────────────────────┐
│                   Repository Layer                     │
│                    PlantRepository                     │
└───────────────────────────▲────────────────────────────┘
              ┌─────────────┴─────────────┐
              ▼                           ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│     Local Data Layer      │ │      Remote Cloud Layer   │
│   Room Database (SQLite)  │ │   Firebase Realtime Sync  │
│  PlantDao & TypeConverters│ │   OkHttp REST & Backup    │
└───────────────────────────┘ └───────────────────────────┘
```

### Core Technologies
- **Language:** [Kotlin 2.0](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Database:** [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite) with KSP
- **Navigation:** Jetpack Navigation Compose with type-safe route parameters
- **Networking:** [OkHttp](https://square.github.io/okhttp/)
- **Document Handling:** Android `PdfDocument` graphics rendering & `FileProvider`

---

## 📁 Project Directory Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── MainActivity.kt               # NavHost, edge-to-edge handling & root theme
│   ├── data/
│   │   ├── Plant.kt                  # Room Entities: Plant, HealthIssue, PlantPdf
│   │   ├── PlantDao.kt               # Reactive Room DAO with Kotlin Flow queries
│   │   ├── PlantDatabase.kt          # Room SQLite Database singleton
│   │   ├── PlantRepository.kt        # Repository bridging DAO and ViewModel
│   │   ├── BackupHelper.kt           # JSON Backup & Restore with Base64 encoding
│   │   ├── FirebaseSyncHelper.kt     # Firebase authentication & Realtime sync
│   │   └── UserSettings.kt           # User preferences, credentials & theme states
│   ├── i18n/
│   │   └── I18n.kt                   # Dynamic English (EN) & Bengali (BN) engine
│   └── ui/
│       ├── PlantViewModel.kt         # Centralized ViewModel & UI state handling
│       ├── screens/
│       │   ├── DashboardScreen.kt    # Home dashboard with statistics & categories
│       │   ├── AllPlantsScreen.kt    # Filterable & searchable plant gallery
│       │   ├── PlantDetailScreen.kt  # Full plant profile & care instructions
│       │   ├── PlantEditScreen.kt    # Plant creation & modification form
│       │   ├── LoginRegisterScreen.kt# Cloud authentication & sync configuration
│       │   ├── PdfShareHelper.kt     # Native A4 PDF renderer & sharing utility
│       │   └── CommonComposables.kt  # Modular, reusable Material 3 widgets
│       └── theme/
│           ├── Color.kt              # Botanical emerald color palette
│           ├── Theme.kt              # Material 3 dynamic color & dark theme setup
│           └── Type.kt               # M3 typography configurations
└── res/                              # Drawables, mipmaps, strings, and XML configs
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or higher
- **JDK:** Version 11 or higher
- **Android Device / Emulator:** Running Android 7.0 (API Level 24) or above

### Installation & Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/plantcare-pro.git
   cd plantcare-pro
   ```
2. **Open the project in Android Studio:**
   - Launch Android Studio and click **Open**.
   - Select the root `plantcare-pro` folder.
   - Wait for Gradle to finish indexing dependencies.
3. **Assemble the APK:**
   ```bash
   gradle assembleDebug
   ```
4. **Deploy & Run:**
   - Select your connected device or emulator in Android Studio and press **Run** (`Shift + F10`).

---

## ⚙️ Configuration

### 1. Offline Mode (Default)
PlantCare Pro works right out of the box with zero external configuration required. All data is written to the on-device SQLite database via Room.

### 2. Firebase Cloud Sync (Optional)
To enable multi-device synchronization:
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Firebase Realtime Database** and **Firebase Authentication** (Email/Password).
3. In the app, tap the **Account** icon on the top bar and enter:
   - **Firebase Project ID**
   - **Firebase Web API Key**
4. Register or log in to sync your local garden records with the cloud.

### 3. Local JSON Backup & Restore
- Tap **Export Backup** from the dashboard menu to export your entire database (including embedded images and attached PDF guides) as an encrypted JSON backup.
- Tap **Import Backup** to restore your data onto any new device.

---

## 🤝 Contributing

Contributions are warmly welcome! If you would like to help improve PlantCare Pro:

1. **Fork the Repository**
2. **Create your Feature Branch:**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit your Changes:**
   ```bash
   git commit -m 'Add some amazing feature'
   ```
4. **Push to the Branch:**
   ```bash
   git push origin feature/amazing-feature
   ```
5. **Open a Pull Request**

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more information.
