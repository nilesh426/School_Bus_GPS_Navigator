# AI Coding Agent Guidelines for SchoolBusApp

## Architecture Overview
SchoolBusApp is a Kotlin Android app for school bus management with role-based access (Admin, Driver, Parent). It uses Firebase Auth for authentication, Firestore for persistent data (students, buses), and Realtime Database for live bus locations. Key components:
- **Activities**: Role-specific dashboards (e.g., `AdminDashboardActivity.kt`) with navigation to CRUD operations.
- **Services**: `LocationForegroundService.kt` for driver location sharing via Realtime DB.
- **Data Models**: Simple Kotlin data classes like `Bus.kt` and `BoardingStudent.kt`.
- **Adapters**: RecyclerView adapters (e.g., `BusAdapter.kt`) for lists.

## Key Patterns
- **Role-Based Navigation**: After login in `LoginActivity.kt`, fetch user role from Firestore `users/{uid}` and redirect to appropriate dashboard (e.g., `AdminDashboardActivity.kt`).
- **Realtime Updates**: Use Firestore snapshot listeners for live counts (e.g., students/buses in `AdminDashboardActivity.kt`). For bus activity, query Realtime DB timestamps (active if <15s old).
- **Location Sharing**: Drivers start `LocationForegroundService` with `busId` extra; service updates Realtime DB `/buses/{busId}` with lat/lng/timestamp every 5s.
- **Data Storage**: Students/buses in Firestore collections; live locations in Realtime DB. Use Firebase BOM for consistent versions.
- **UI Conventions**: Activities extend `AppCompatActivity`, use `findViewById` for views, Material buttons. Toolbars with logout menu (see `AdminDashboardActivity.kt`).

## Developer Workflows
- **Build**: Use `./gradlew build` or Android Studio. Dependencies managed via `gradle/libs.versions.toml` version catalog.
- **Run/Debug**: Deploy via Android Studio or `./gradlew installDebug`. Enable location permissions in emulator/device.
- **Testing**: Unit tests with JUnit in `test/`, instrumentation with Espresso in `androidTest/`. Run via `./gradlew testDebugUnitTest`.
- **Firebase Setup**: Add `google-services.json` to `app/`; configure Auth, Firestore, Realtime DB in Firebase Console.

## Integration Points
- **Google Maps**: API key in `AndroidManifest.xml`; used in map activities (e.g., `AdminMapActivity.kt`) for bus tracking.
- **Permissions**: Location (fine/coarse), foreground service in manifest; request at runtime if needed.
- **External Deps**: Firebase (auth/firestore/database), Google Play Services (location/maps). Update via `app/build.gradle.kts`.

Reference: `app/build.gradle.kts` for deps, `LoginActivity.kt` for auth flow, `LocationForegroundService.kt` for location logic.
