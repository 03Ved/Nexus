# Nexus — Campus Communication for Android

**A native Android campus community app for finding collaborators, coordinating activities, and keeping conversations connected to the posts that started them.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)

> **Project status:** Academic mini-project / functional prototype. Tested on an Android 14 device. Some features and production-hardening work remain incomplete; see [Known limitations](#known-limitations).

## Why Nexus?

Campus announcements, event plans, and requests for collaborators often get scattered across chat groups and notice boards. Nexus provides a structured way to publish a campus activity, discover relevant posts, and join a discussion linked to that activity.

## Features

- **Intent-based feed:** Create and filter posts as **Looking For**, **Happening**, **Asking**, or **Offering**; interact with posts through likes and comments.
- **Contextual group chats:** Join the discussion associated with a post using the **I'm In** action; messages are backed by Cloud Firestore.
- **Campus map:** Discover location-tagged posts using GPS and an OpenStreetMap-powered map.
- **Account management:** Email/password sign-in and registration with Firebase Authentication.
- **Local persistence:** Room database for local post storage and DataStore for preferences/session-related state.
- **Background work:** WorkManager job for synchronizing pending posts, cleaning up expired local posts, and refreshing messaging tokens.
- **Notification infrastructure:** Firebase Cloud Messaging service and notification handling; automated event-triggered delivery is not fully implemented.
- **Reactive UI:** Jetpack Compose, ViewModels and StateFlow.

## Screenshots

| Feed | Post Details | Campus Map |
|:---:|:---:|:---:|
| <img src="docs/screenshots/Screenshot 2026-10-30 133652.png" width="240" alt="Feed screen"> | <img src="docs/screenshots/Screenshot 2026-10-30 133729.png" width="240" alt="Post details screen"> | <img src="docs/screenshots/Screenshot 2026-10-30 133746.png" width="240" alt="Campus map screen"> |

| Chats | Profile |
|:---:|:---:|
| <img src="docs/screenshots/Screenshot 2026-10-30 133801.png" width="240" alt="Chats screen"> | <img src="docs/screenshots/Screenshot 2026-10-30 133815.png" width="240" alt="Profile screen"> |


## Tech stack

| Layer | Technologies |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material Design 3 |
| Architecture | MVVM-style ViewModels, repository pattern, Hilt dependency injection |
| Cloud services | Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging |
| Local data | Room (SQLite), DataStore Preferences |
| Background processing | WorkManager, Kotlin Coroutines |
| Networking | Retrofit, OkHttp, Gson |
| Maps / location | osmdroid (OpenStreetMap), Fused Location Provider, Android Geocoder |
| Image loading | Coil |

## Architecture

```text
Jetpack Compose screens
        │
        ▼
ViewModels + StateFlow
        │
        ▼
Repository layer (Auth / Posts / Chats / Location)
        ├────────► Firebase Auth + Cloud Firestore
        ├────────► Room local database
        └────────► Android location services / networking

WorkManager (SyncWorker) ──► Pending post sync + local cleanup
Firebase Messaging Service ──► Notification handling
```

The code is organized under `app/src/main/java/com/example/nexus/`:

```text
ui/           Compose screens, components, navigation and ViewModels
 domain/      Domain models for posts, chats, messages and users
 data/        Repositories, Room entities/DAOs, remote API models and mappers
 di/          Dependency injection modules
 service/     Firebase messaging and background synchronization services
 worker/      Additional worker-related code
 MainActivity.kt   App entry point and navigation
 NexusApp.kt       Application-level initialization
```

### Example flows

**Opening the feed:** Compose UI → `FeedViewModel` → `PostRepository` → Firestore-backed posts → reactive UI state.

**Offline post synchronization:** Room stores locally queued posts → `SyncWorker` invokes repository synchronization → Firestore receives pending records when synchronization succeeds. Full offline-first support is still incomplete.

## Run locally

### Requirements

- Android Studio with an Android SDK supporting **compile SDK 35**
- JDK compatible with the included Gradle/Android Gradle Plugin configuration
- Android emulator or physical device running **Android 8.0 (API 26) or newer**
- Your own Firebase project to enable cloud-backed features

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/03Ved/Nexus.git
   cd Nexus
   ```

2. Open the project root in **Android Studio** and allow Gradle sync to complete.
3. Create a Firebase project and register an Android app with package/application ID **`com.example.nexus`** (or change the application ID and use a matching Firebase configuration).
4. Download your project's **`google-services.json`** and place it at **`app/google-services.json`**. The repository snapshot may contain an existing configuration: replace it with your own. Never upload private service-account keys or credentials.
5. Enable **Email/Password** in Firebase Authentication and create a **Cloud Firestore** database. Configure appropriate Firestore security rules for your own development environment; do not use open production rules.
6. Run the `app` configuration on an emulator or physical Android device. For map/location features, enable location permissions when prompted.

**Optional integrations:** Google sign-in is not wired through the visible login button, and automated FCM notification triggers need additional backend infrastructure. No Google Maps API key is required for the app's osmdroid/OpenStreetMap map display; `MAPS_API_KEY` appears as a build manifest placeholder.

> **Build verification:** The source and accompanying report were reviewed for this README; this generated document does not claim a fresh successful build on your machine.

## Testing

The academic project report documents **18 manual test cases**, including authentication, post creation, comments, chats, map markers, offline post synchronization, notifications, and session persistence. The report records these cases as passing on a OnePlus device running Android 14.

The repository includes basic Android test scaffolding but **does not yet include comprehensive automated coverage**. Before production use, add regression tests for authentication validation, repository operations, offline synchronization, and UI flows.

## Known limitations

- Email-domain validation requires hardening; do not treat the current client-side check as secure college-membership verification.
- Google sign-in's visible UI entry point is unfinished.
- Images stored as device-local paths are not reliably viewable on other devices.
- Automatic event-driven push notifications are not implemented; notifications were demonstrated using Firebase Console testing.
- Offline caching and sync exist, but some offline-first behavior and draft-saving functionality remain incomplete.
- The college event API integration is scaffolded rather than a fully deployed external service.
- Production security rules, automated tests, error handling and real-device coverage need further review.

## What I would improve next

1. Harden authentication and implement server-enforced membership checks.
2. Complete draft persistence and strengthen bidirectional offline synchronization.
3. Implement shared media storage and safe image URLs.
4. Add reliable event-triggered notifications and automated tests.
5. Document reproducible performance measurements and expand device testing.
