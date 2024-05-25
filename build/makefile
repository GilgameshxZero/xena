# GNU make (gmake) & nmake shared makefile.
# Conditional logic implemented with differing end-of-line comment syntax.
# To enable multithreaded compilation, nmake compiles all files together with
# /MP, while gmake compiles separately with -jN.

# Default target defined before any "fake" targets.
__default: _default

# ------------------------------------------------------------------------------
ifdef MAKEDIR: # gmake: false; nmake: unused target
!ifdef MAKEDIR # gmake: not seen; nmake: true
# ------------------------------------------------------------------------------

# nmake specific commands.
!IFNDEF JAVA_SOURCE_VERSION
JAVA_SOURCE_VERSION = 17
!ENDIF
!IFNDEF BUILD_ANDROID_VERSION
BUILD_ANDROID_VERSION = 34
!ENDIF
BUILD_TOOLS_DIR = $(ANDROID_HOME)\build-tools\$(BUILD_ANDROID_VERSION).0.0^\
PLATFORM_TOOLS_DIR = $(ANDROID_HOME)\platform-tools^\
PLATFORM_DIR = $(ANDROID_HOME)\platforms\android-$(BUILD_ANDROID_VERSION)^\

!IFNDEF BUILD
BUILD = 0
!ENDIF
!IF $(BUILD) == 1
BUILD_NAME = release
!ELSE
BUILD_NAME = debug
!ENDIF
PROJECT_NAME = xena
PACKAGE_PATH = com\gilgamesh\$(PROJECT_NAME)
PACKAGE_NAME = com.gilgamesh.$(PROJECT_NAME)
MAIN_CLASS = FilePickerActivity
ROOT_DIR = $(MAKEDIR)\..^\
# All library files to be declared here.
LIB_NAME_MARKER_VERSION = 1.6.47.2
LIB_NAMES = \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-base/$(LIB_NAME_MARKER_VERSION):onyxsdk-base-$(LIB_NAME_MARKER_VERSION) \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-device/1.2.24:onyxsdk-device-1.2.24 \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-pen/1.4.7:onyxsdk-pen-1.4.7 \
# repo1.maven.org/maven2/io/reactivex/rxjava/1.3.8:rxjava-1.3.8

# Real targets.
# Default/build target only compiles and links, but does not run.
_default: build

# All libraries are made together. One file is specified as target to track
# dependency, since tracking a directory is not possible.
$(ROOT_DIR)lib\onyxsdk-base-$(LIB_NAME_MARKER_VERSION).jar:
	@IF EXIST $(ROOT_DIR)lib RD /Q /S $(ROOT_DIR)lib
	@MD $(ROOT_DIR)lib
	@FOR %%I IN ($(LIB_NAMES)) DO @( \
		@FOR /F "tokens=1-2 delims=:" %%J IN ("%%I") DO @( \
			@ECHO Downloading $(ROOT_DIR)lib\%%K.jar... && \
			BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
				/DOWNLOAD /PRIORITY FOREGROUND \
				"https://%%J/%%K.aar" \
				"$(ROOT_DIR)lib\%%K.aar" && \
			jar xf $(ROOT_DIR)lib\%%K.aar classes.jar && \
			MOVE classes.jar $(ROOT_DIR)lib\%%K.jar >NUL && \
			DEL $(ROOT_DIR)lib\%%K.aar \
		) \
	)

# Create R.java.
$(ROOT_DIR)src\$(PACKAGE_PATH)\R.java: $(ROOT_DIR)res\** $(ROOT_DIR)res\layout\* $(ROOT_DIR)res\values\*
	@ECHO Creating $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java...
	@IF NOT EXIST $(ROOT_DIR)obj MD $(ROOT_DIR)obj
	@CALL $(BUILD_TOOLS_DIR)aapt.exe package -f -m \
		-S "$(ROOT_DIR)res" \
		-J "$(ROOT_DIR)src" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-I $(PLATFORM_DIR)android.jar

$(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore:
	@ECHO Creating $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore...
	@CALL "$(JAVA_HOME)\bin\keytool" -genkey -validity 10000 \
		-dname "CN=AndroidDebug, O=Android, C=US" \
		-keystore $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore \
		-storepass android -keypass android \
		-alias android_debug_key -keyalg RSA -keysize 2048

# Compile target: .java to .class. Only sources directly under the main path are compiled, for now.
$(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class: $(ROOT_DIR)lib\onyxsdk-base-$(LIB_NAME_MARKER_VERSION).jar $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java $(ROOT_DIR)src\$(PACKAGE_PATH)\*.java
	@ECHO Compiling to .class...
	@DIR /S /B $(ROOT_DIR)src\*.java > src.txt
	@CALL "$(JAVA_HOME)\bin\javac" \
		-source $(JAVA_SOURCE_VERSION) \
		-target $(JAVA_SOURCE_VERSION) \
		-d "$(ROOT_DIR)obj" \
		-cp "$(PLATFORM_DIR)android.jar";"$(ROOT_DIR)lib\*" \
		-sourcepath "$(ROOT_DIR)src" \
		-Xlint:deprecation \
		@src.txt
	@DEL src.txt

# Build target. R.class is used as dependency alias for compilation.
$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk: $(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class $(ROOT_DIR)priv\$(PROJECT_NAME).$(BUILD_NAME).keystore $(ROOT_DIR)AndroidManifest.xml
	@ECHO Building to $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk...
	@IF NOT EXIST $(ROOT_DIR)bin MD $(ROOT_DIR)bin
# Generate APK.
	@CALL $(BUILD_TOOLS_DIR)aapt.exe package -f -m \
		-F "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-S "$(ROOT_DIR)res" \
		-I $(PLATFORM_DIR)/android.jar
# Convert to DEX.
	@CALL $(BUILD_TOOLS_DIR)d8.bat \
		--lib "$(PLATFORM_DIR)android.jar" \
		--min-api 34 \
		--$(BUILD_NAME) \
		--output $(ROOT_DIR)bin \
		$(ROOT_DIR)obj\$(PACKAGE_PATH)\*.class \
		$(ROOT_DIR)lib\*.jar
# @DIR /S /B $(ROOT_DIR)lib\*.jar > lib.txt
# --classpath @lib.txt \
# -j "$(ROOT_DIR)lib\onyxsdk-base-1.7.2.jar" \
# -j "$(ROOT_DIR)lib\onyxsdk-device-1.2.29.jar" \
# -j "$(ROOT_DIR)lib\onyxsdk-pen-1.2.1.jar" \
# @DEL lib.txt
# For some reason, aapt add only works for classes.dex in the current directory
	@MOVE "$(ROOT_DIR)bin\classes.dex" . >NUL
	@CALL $(BUILD_TOOLS_DIR)aapt.exe add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		classes.dex
	@DEL classes.dex
# Add shared dependencies.
	@MKDIR lib lib\arm64-v8a
	@COPY "$(ROOT_DIR)so" lib\arm64-v8a >NUL
	@CALL $(BUILD_TOOLS_DIR)aapt.exe add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		lib/arm64-v8a/libonyx_pen_touch_reader.so \
		lib/arm64-v8a/libc++_shared.so
	@RD /Q /S lib
# Align to 4-byte boundaries.
	@CALL $(BUILD_TOOLS_DIR)zipalign.exe -f 4 \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk"
# Sign package.
	@CALL "$(BUILD_TOOLS_DIR)apksigner.bat" sign \
		--ks $(ROOT_DIR)priv\$(PROJECT_NAME).$(BUILD_NAME).keystore \
		--ks-pass pass:android \
		--ks-key-alias android_debug_key \
		--key-pass pass:android \
		--out $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk \
		$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"

build: $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk

# Build and run.
run: $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk
	@ECHO Uninstalling from device...
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe uninstall $(PACKAGE_NAME) &
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe install -r $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe shell am start $(PACKAGE_NAME)/$(PACKAGE_NAME).$(MAIN_CLASS)

# Clean target removes all intermediates.
clean:
	@IF EXIST $(ROOT_DIR)lib RD /Q /S $(ROOT_DIR)lib
	@IF EXIST $(ROOT_DIR)bin RD /Q /S $(ROOT_DIR)bin
	@IF EXIST $(ROOT_DIR)obj RD /Q /S $(ROOT_DIR)obj
	@IF EXIST $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore \
		DEL $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore
	@IF EXIST $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java \
		DEL $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java
	@ECHO Uninstalling from device...
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe uninstall $(PACKAGE_NAME) &

# ------------------------------------------------------------------------------
!else
else
# ------------------------------------------------------------------------------

# GNU make (gmake) specific commands.

# Ensure files with the same name as fake targets do not trigger.
.PHONY: __default _default
_default: 
	@echo "`gmake`/Linux not supported. Use `nmake`/Windows instead.""

# ------------------------------------------------------------------------------
endif    # gmake: close condition; nmake: not seen
!endif : # gmake: unused target; nmake close conditional
# ------------------------------------------------------------------------------
