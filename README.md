# Smart Courier

Offline-first Android delivery management app built with Clean Architecture, MVVM, Room, Firebase, and Compose.

## Architecture

- **Domain** — Pure Kotlin models, repository interfaces, use cases
- **Data** — Room (SSOT), Firestore (sync target), outbox queue, WorkManager sync engine
- **Feature** — Auth, Dashboard, Route Planner, Active Delivery HUD

Multi-country support (AE, SA, OM, PK, PH), telemetry via RTDB, customer tracking via Leaflet.js portal.

## Build

```bash
./gradlew assembleRelease
```

Requires `app/google-services.json` from Firebase Console.

## CI

GitHub Actions runs lint, compilation, unit tests, and emulator-based E2E tests on every push to `main`/`develop`.

## License

MIT
