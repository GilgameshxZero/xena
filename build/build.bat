@ECHO OFF

SET BUILD_TOOLS_DIR=%ANDROID_HOME%/build-tools/34.0.0
SET PLATFORM_TOOLS_DIR=%ANDROID_HOME%/platform-tools
SET PLATFORM_DIR=%ANDROID_HOME%/platforms/android-34

@REM SET AAPT_PATH=%ANDROID_HOME%\build-tools\34.0.0\aapt.exe
@REM SET ADB_PATH=%ANDROID_HOME%\platform-tools\adb.exe
@REM SET ANDROID_JAR=%ANDROID_HOME%\platforms\android-34\android.jar
@REM SET DX_PATH=%ANDROID_HOME%\build-tools\34.0.0\lib\d8.jar

SET ROOT_DIR=%CD%/..
SET PACKAGE_PATH=com/gilgamesh/xenagogy

rm -rf %ROOT_DIR%/obj
rm -rf %ROOT_DIR%/bin
mkdir %CD%\..\obj
mkdir %CD%\..\bin

@REM Create R.java.
CALL %BUILD_TOOLS_DIR%/aapt.exe package -f -m -S "%ROOT_DIR%/res" -J "%ROOT_DIR%/src" -M "%ROOT_DIR%/AndroidManifest.xml" -I %PLATFORM_DIR%/android.jar

@REM Compile Java code.
CALL "%JAVA_HOME%/bin/javac" -source 1.10 -target 1.10 -d "%ROOT_DIR%/obj" -cp "%PLATFORM_DIR%/android.jar";"%ROOT_DIR%/include/onyxsdk-base-1.6.51.jar";"%ROOT_DIR%/include/onyxsdk-device-1.2.29.jar";"%ROOT_DIR%/include/onyxsdk-pen-1.4.8.jar" -sourcepath "%ROOT_DIR%/src" "%ROOT_DIR%/src/%PACKAGE_PATH%/MainActivity.java" "%ROOT_DIR%/src/%PACKAGE_PATH%/R.java"
@REM %ROOT_DIR%/include/support-annotations-26.0.1.jar
@REM %ROOT_DIR%/include/appcompat-v7-28.0.0.jar
@REM %ROOT_DIR%/include/butterknife-7.0.1.jar
@REM -processor "com.jakewharton:butterknife-compiler:8.8.1"

@REM Convert to DEX.
CALL %BUILD_TOOLS_DIR%/d8.bat --lib "%PLATFORM_DIR%/android.jar" --classpath "%ROOT_DIR%/include/onyxsdk-base-1.6.51.jar";"%ROOT_DIR%/include/onyxsdk-device-1.2.29.jar";"%ROOT_DIR%/include/onyxsdk-pen-1.4.8.jar" --min-api 34 --release --output %ROOT_DIR%/bin %ROOT_DIR%/obj/com/gilgamesh/xenagogy/MainActivity.class %ROOT_DIR%/obj/com/gilgamesh/xenagogy/R.class

@REM Generate APK.
CALL %BUILD_TOOLS_DIR%/aapt.exe package -f -m -F "%ROOT_DIR%/bin/Xenagogy.unsigned.apk" -M "%ROOT_DIR%/AndroidManifest.xml" -S "%ROOT_DIR%/res" -I %PLATFORM_DIR%/android.jar
@REM For some reason, aapt add only works for classes.dex in the current directory
cp "%ROOT_DIR%/bin/classes.dex" .
CALL %BUILD_TOOLS_DIR%/aapt.exe add "%ROOT_DIR%/bin/Xenagogy.unsigned.apk" classes.dex
rm classes.dex

@REM Align to 4-byte boundaries.
CALL %BUILD_TOOLS_DIR%/zipalign.exe -f 4 "%ROOT_DIR%/bin/Xenagogy.unsigned.apk" "%ROOT_DIR%/bin/Xenagogy.unsigned.aligned.apk"

@REM Create key and sign package.
@REM put >NULL
rm %ROOT_DIR%/priv/Xenagogy.keystore
CALL "%JAVA_HOME%/bin/keytool" -genkey -validity 10000 -dname "CN=AndroidDebug, O=Android, C=US" -keystore %ROOT_DIR%/priv/Xenagogy.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048
CALL "%BUILD_TOOLS_DIR%/apksigner.bat" sign --ks %ROOT_DIR%/priv/Xenagogy.keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out %ROOT_DIR%/bin/Xenagogy.signed.apk %ROOT_DIR%/bin/Xenagogy.unsigned.aligned.apk
