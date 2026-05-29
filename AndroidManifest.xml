# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line number and source file information for stack traces
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Disable obfuscation entirely to prevent package/class renaming,
# which preserves original names for reflection and makes debugging release builds easy.
# -dontobfuscate

# Remove Verbose and Debug Logs for Release builds (keeps Info, Warn, and Error logs)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-keep class com.sr.openbyd.proxy.EntryPoint { *; }
-keep class com.sr.openbyd.proxy.** { *; }
-keep class com.sr.openbyd.ipc.** { *; }

# Keep the BYD SDK stub classes and interfaces completely untouched.
# This prevents R8 from statically optimizing and inlining method calls (like getInstance returning null),
# ensuring that the JVM dynamically loads the real implementations from the system at runtime.
-keep class android.hardware.bydauto.** { *; }
-keep class android.hardware.IBYDAuto** { *; }

# Keep apksig library classes and members completely untouched.
# apksig relies heavily on reflection (e.g., in Asn1DerEncoder) to encode ASN.1 signature blocks,
# which breaks in release builds if classes, fields, or annotations are obfuscated or removed.
-keep class com.android.apksig.** { *; }

# Keep Bouncy Castle classes and members completely untouched.
# Bouncy Castle is used for cryptographic operations, key generation, and signing.
# It uses reflection for loading security providers.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**