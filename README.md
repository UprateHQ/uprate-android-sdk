<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/logo-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="assets/logo.svg">
    <img alt="UprateHQ" src="assets/logo.svg" width="200">
  </picture>
</p>

<p align="center">
  <strong>UprateSDK for Android</strong><br>
  Roadmap voting, in-app feedback, and review insights for your Android app.
</p>

<p align="center">
  <a href="https://app.upratehq.com">Dashboard</a> &bull;
  <a href="https://upratehq.com">Website</a> &bull;
  <a href="#quick-start">Quick Start</a>
</p>

---

## Requirements

- Android API 24+ (Android 7.0)
- Kotlin 2.0+

## Installation

### JitPack

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.UprateHQ:uprate-sdk-android:0.1.0")
}
```

## Quick Start

### 1. Get Your API Key

Create a publishable API key in your [UprateHQ dashboard](https://app.upratehq.com). Navigate to your app's **Settings > SDK** to generate one.

### 2. Configure the SDK

Call `configure` once at app launch in your `Application.onCreate()`, before using any features:

```kotlin
import com.upratehq.sdk.UprateSDK

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UprateSDK.configure(
            context = this,
            apiKey = "uprt_pub_your_publishable_key"
        )
    }
}
```

### 3. Set User Context

After the user logs in, set their identity:

```kotlin
UprateSDK.instance.setUserContext(
    userId = "user_123",
    email = "jane@example.com",
    name = "Jane"
)
```

Call `clearUserContext()` on logout.

### 4. Use Features

#### Roadmap

```kotlin
// Fetch roadmap items
val roadmap = UprateSDK.instance.roadmap.getItems()

// Vote on an item
val result = UprateSDK.instance.roadmap.vote(itemId = item.id)

// Submit a feature request
val request = UprateSDK.instance.roadmap.submitRequest(
    title = "Dark mode",
    description = "Please add dark mode support"
)
```

#### Feedback

```kotlin
// Submit feedback with optional rating
val result = UprateSDK.instance.feedback.submit(
    message = "Checkout is slow but the app looks great",
    rating = 4,
    metadata = mapOf("screen" to "checkout")
)

// List user's submissions
val submissions = UprateSDK.instance.feedback.getMySubmissions()
```

#### Review Signals

Record when you show the Google Play review prompt to enrich review data in your dashboard:

```kotlin
val manager = ReviewManagerFactory.create(context)
val request = manager.requestReviewFlow()
request.addOnCompleteListener {
    if (it.isSuccessful) {
        manager.launchReviewFlow(activity, it.result)
        lifecycleScope.launch {
            try { UprateSDK.instance.reviews.recordPrompt() } catch (_: Exception) { }
        }
    }
}
```

## Features

- **Roadmap & Voting** — Let users browse your roadmap and vote on features
- **In-App Feedback** — Collect feedback with optional ratings and device metadata
- **Review Signals** — Match Google Play reviews to users for richer insights
- **Coroutines** — Modern Kotlin coroutines with `suspend` functions throughout
- **Lightweight** — Minimal dependencies: OkHttp and kotlinx.serialization
- **Testable** — Interface-based design with `RoadmapProviding`, `FeedbackProviding`, and `ReviewSignalProviding` for easy mocking
- **Privacy-Conscious** — No tracking, no advertising identifiers, no fingerprinting. Device metadata (model, OS version) is collected only with feedback and review signals, and can be disabled

## Testing

The SDK provides interfaces for each feature module, making it easy to mock in your tests:

```kotlin
class MockRoadmap : RoadmapProviding {
    override suspend fun getItems(): UprateRoadmapResponse { ... }
    override suspend fun vote(itemId: String): VoteResult { ... }
    // ...
}
```

## Privacy

- **No tracking** — no advertising identifiers, no fingerprinting
- **Permission** — INTERNET only
- **Device metadata** (model, OS version, locale, RAM, free disk) is collected with feedback submissions and review signals. Disable it per-feature with `feedback.collectDeviceMetadata = false`
- **User identity** (`userId`, `email`, `name`) is provided by your app — the SDK does not gather it independently
- **No local storage** — all data is transient, fetched on demand

## License

UprateSDK is available under the MIT License. See [LICENSE](LICENSE) for details.
