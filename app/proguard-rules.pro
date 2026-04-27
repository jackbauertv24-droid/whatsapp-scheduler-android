-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.example.wascheduler.data.** { *; }
-keep class com.example.wascheduler.bridge.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers enum * {
    public static **[] values();
    public ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-dontwarn okhttp3.**
-dontwarn okio.**