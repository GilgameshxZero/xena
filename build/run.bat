@ECHO OFF

SET BUILD_TOOLS_DIR=%ANDROID_HOME%/build-tools/34.0.0
SET PLATFORM_TOOLS_DIR=%ANDROID_HOME%/platform-tools
SET PLATFORM_DIR=%ANDROID_HOME%/platforms/android-34

SET ROOT_DIR=%CD%/..
SET PACKAGE=com.gilgamesh.xenagogy
SET MAIN_CLASS=MainActivity

CALL %PLATFORM_TOOLS_DIR%/adb.exe uninstall %PACKAGE%
CALL %PLATFORM_TOOLS_DIR%/adb.exe install -r %ROOT_DIR%/bin/Xenagogy.signed.apk
CALL %PLATFORM_TOOLS_DIR%/adb.exe shell am start %PACKAGE%/%PACKAGE%.%MAIN_CLASS%
