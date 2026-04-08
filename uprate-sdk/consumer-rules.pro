# Keep serializable models for kotlinx.serialization
-keep class com.upratehq.sdk.models.** { *; }
-keepclassmembers class com.upratehq.sdk.models.** { *; }

# Keep companion objects with serializer()
-keepclassmembers class com.upratehq.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class com.upratehq.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep internal serializable classes in networking package
-keep class com.upratehq.sdk.networking.DeviceMetadata { *; }
-keep class com.upratehq.sdk.networking.ErrorResponse { *; }
-keep class com.upratehq.sdk.networking.ValidationErrorResponse { *; }
