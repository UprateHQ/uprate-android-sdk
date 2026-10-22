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

Add the JitPack repository to your app's `settings.gradle.kts`:

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
    implementation("com.github.UprateHQ:uprate-android-sdk:0.1.2")
}
```

If the repository or JitPack artifact is private, add your JitPack token to
`~/.gradle/gradle.properties`:

```properties
authToken=YOUR_JITPACK_TOKEN
```

Then configure the JitPack repository with credentials:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            credentials {
                username = providers.gradleProperty("authToken").orNull
                password = ""
            }
        }
    }
}
```

Do not commit JitPack tokens to your repository.

## Quick Start

### 1. Get Your SDK Key

In your [UprateHQ dashboard](https://app.upratehq.com), open **Apps**, find your app, and choose **SDK API Keys** to generate a publishable key. It starts with `uprt_pub_` — that is the one the SDK needs.

> **Heads up:** keys from **Settings > API keys** are for the REST API and won't work with the SDK. The secret key (`uprt_secret_`) from the SDK key pair belongs on your server, never in the app.

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
    name = "Jane",
    userToken = userTokenFromYourServer
)
```

Call `clearUserContext()` on logout.

`userToken` is optional. Generate it on your server as the hex HMAC-SHA256 of the
user ID, signed with your SDK secret key. The SDK sends it as `X-SDK-User-Token`
on every request until the context is cleared or replaced. Never put the secret
key in your app. See [Verify users](https://upratehq.com/docs/sdk#verify-users).

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

#### Replies in your app

Fetch unread replies when your activity resumes. This example uses your app's
AndroidX `lifecycleScope`; the SDK adds no lifecycle dependency or observers.
`showRepliesAndAwaitDismissal` is your app's suspending UI function: return only
after the user has read the replies, then mark them read.

```kotlin
import androidx.lifecycle.lifecycleScope
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.launch

override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
        try {
            val feedback = UprateSDK.instance.feedback
            val replies = feedback.getUnreadReplies()
            if (replies.isNotEmpty()) {
                showRepliesAndAwaitDismissal(replies)
                feedback.markRead(replies)
            }
        } catch (error: UprateError) {
            handleReplyError(error)
        }
    }
}
```

`getUnreadReplies()` returns up to 100 replies, oldest first. `markRead(replies)`
marks those replies read; an empty list makes no request. Feedback history also
includes each submission's `replies`, defaulting to an empty list on older servers.
Each `FeedbackReply` has `id`, `feedbackId`, `body`, `createdAt`, and nullable
`readAt`; timestamps are ISO 8601 strings.

If the user context changes during one of these requests or `getMySubmissions()`, the SDK
throws `UprateError.UserContextChanged` instead of returning the previous user's result.
Dismiss any replies already shown when the user logs out or switches accounts.

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

## Errors

SDK calls throw `UprateError`. `UserTokenRejected(message)` means the server requires
a user token or rejected the supplied token; `message` preserves the server's
explanation. Other 401 responses still throw `InvalidApiKey`. `UserContextChanged`
means a replies request completed after the context changed; discard it and fetch
again for the current user. `UserContextNotSet` means you need to set the user's
identity before calling a feature.

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
- **User identity** (`userId`, `email`, `name`, optional `userToken`) is provided by your app — the SDK does not gather it independently
- **No local storage** — all data is transient, fetched on demand

## License

UprateSDK is available under the MIT License. See [LICENSE](LICENSE) for details.
