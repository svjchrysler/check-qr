# El parser se configura con plantillas JSON, asi que sus tipos serializables
# tienen que sobrevivir a R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.seef.checkqr.**$$serializer { *; }
-keepclassmembers class com.seef.checkqr.** {
    *** Companion;
}
-keepclasseswithmembers class com.seef.checkqr.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Tink (verificacion Ed25519 de las plantillas firmadas)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
