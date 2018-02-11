-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-dontwarn com.google.android.gms.**
-dontwarn com.google.common.cache.**
-dontwarn com.google.common.primitives.**

# Adel
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *;}

-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *;}

-dontwarn com.googlecode.mp4parser.**
-keep class com.googlecode.mp4parser.** { *;}

-keep class org.telegram.tgnet.** { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.PhoneFormat.** { *; }

#-keep class org.telegram.messenger.audioinfo.** { public private *; }
#-keep class org.telegram.messenger.browser.** { public private *; }
#-keep class org.telegram.messenger.camera.** { public private *; }
-keep class org.telegram.messenger.exoplayer2.** { public private *; }
#-keep class org.telegram.messenger.query.** { public private *; }
#-keep class org.telegram.messenger.secretmedia.** { public private *; }
#-keep class org.telegram.messenger.support.** { public private *; }
#-keep class org.telegram.messenger.time.** { public private *; }
-keep class org.telegram.messenger.video.** { public private *; }
-keep class org.telegram.messenger.voip.** { public private *; }
#-keep class org.telegram.messenger.ApplicationLoader { public private *; }
#-keep class org.telegram.messenger.AppStartReceiver { public private *; }
#-keep class org.telegram.messenger.AuthenticatorService { public private *; }
#-keep class org.telegram.messenger.AutoMessageHeardReceiver { public private *; }
#-keep class org.telegram.messenger.AutoMessageReplyReceiver { public private *; }
#-keep class org.telegram.messenger.CallReceiver { public private *; }
#-keep class org.telegram.messenger.ClearCacheService { public private *; }
#-keep class org.telegram.messenger.ContactsSyncAdapterService { public private *; }
#-keep class org.telegram.messenger.DownloadManagerService { public private *; }
#-keep class org.telegram.messenger.GcmInstanceIDListenerService { public private *; }
#-keep class org.telegram.messenger.GcmPushListenerService { public private *; }
#-keep class org.telegram.messenger.GcmRegistrationIntentService { public private *; }
#-keep class org.telegram.messenger.ImageLoader { public private *; }
#-keep class org.telegram.messenger.MediaController { public private *; }
#-keep class org.telegram.messenger.MusicBrowserService { public private *; }
#-keep class org.telegram.messenger.MusicPlayerReceiver { public private *; }
#-keep class org.telegram.messenger.MusicPlayerService { public private *; }
#-keep class org.telegram.messenger.NativeLoader { public private *; }
#-keep class org.telegram.messenger.NotificationDismissReceiver { public private *; }
#-keep class org.telegram.messenger.NotificationRepeat { public private *; }
#-keep class org.telegram.messenger.NotificationsService { public private *; }
#-keep class org.telegram.messenger.OpenChatReceiver { public private *; }
#-keep class org.telegram.messenger.PopupReplyReceiver { public private *; }
#-keep class org.telegram.messenger.ShareBroadcastReceiver { public private *; }
#-keep class org.telegram.messenger.SmsListener { public private *; }
#-keep class org.telegram.messenger.TgChooserTargetService { public private *; }
#-keep class org.telegram.messenger.Utilities { public private *; }
#-keep class org.telegram.messenger.VideoEncodingService { public private *; }
#-keep class org.telegram.messenger.WearReplyReceiver { public private *; }

# Use -keep to explicitly keep any other classes shrinking would remove
-dontoptimize
#-dontshrink
#-dontobfuscate