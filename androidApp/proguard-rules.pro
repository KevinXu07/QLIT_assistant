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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class com.schedule.qlit.IUserService { *; }
-keep class com.schedule.qlit.IUserService$* { *; }
-keep class com.schedule.qlit.system.ShizukuUserService { *; }
-keep class com.schedule.qlit.system.DevtoolsBridge { *; }

# focus-api
-keep class com.xzakota.hyper.** { *; }

# Gson 数据类保留
-keep class com.schedule.qlit.data.model.Course { *; }
-keep class com.schedule.qlit.data.model.CourseMeta { *; }
-keep class com.schedule.qlit.data.model.TimeSlot { *; }
-keep class com.schedule.qlit.data.model.ScheduleData { *; }

# Room 的数据库实现由 Room 通过反射创建；仅保留类本身会让 R8 删除无参构造器。
# WorkManager 的 WorkDatabase_Impl 也属于这个生成类集合。
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# 保留所有字段名，防止 Gson 反射失败
-keepclassmembers class com.schedule.qlit.** {
    <fields>;
}

# 保留泛型类型信息，Map<String, TimeSlot> 需要这个
-keepattributes Signature
-keepattributes *Annotation*

# 防止 Gson 内部类被混淆
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**


# EncryptedSharedPreferences / Tink: 缺失的 javax.annotation 类
-dontwarn javax.annotation.**

# OkHttp（WebSocket + CDP 桥接用）
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
