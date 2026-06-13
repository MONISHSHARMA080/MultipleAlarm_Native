#-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep raw resources
#-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.


-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

-keep class com.coolApps.MultipleAlarmClock.Activities.AlarmActivityIntentData { *; }


