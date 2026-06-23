# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.bensley.fieldrelay.**$$serializer { *; }
-keepclassmembers class com.bensley.fieldrelay.** {
    *** Companion;
}
-keepclasseswithmembers class com.bensley.fieldrelay.** {
    kotlinx.serialization.KSerializer serializer(...);
}
