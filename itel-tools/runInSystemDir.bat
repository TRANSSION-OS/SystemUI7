adb root
adb remount
adb push ../SystemUI7/build/outputs/apk/SystemUI.apk  /system/priv-app/SystemUI7/SystemUI7.apk
adb shell pkill systemui
pause