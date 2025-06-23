# GNU make (gmake) & nmake shared makefile.
# Conditional logic implemented with differing end-of-line comment syntax.
# To enable multithreaded compilation, nmake compiles all files together with
# /MP, while gmake compiles separately with -jN.

# Relevant targets:
# clean
# logcat
# adb (ARGS=)
# build (BUILD=0,1)
# run (BUILD=0,1)

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
!IFNDEF ANDROID_TARGET_VERSION
ANDROID_TARGET_VERSION = 34
!ENDIF
!IFNDEF ANDROID_MINIMUM_VERSION
ANDROID_MINIMUM_VERSION = 28
!ENDIF

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
MAIN_CLASS = filesystem.FilePickerActivity
ROOT_DIR = $(MAKEDIR)\..^\
# All library files to be declared here.
LIB_NAMES = \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-base/1.7.6:onyxsdk-base-1.7.6:aar \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-device/1.2.30:onyxsdk-device-1.2.30:aar \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-pen/1.4.11:onyxsdk-pen-1.4.11:aar \
repo1.maven.org/maven2/io/reactivex/rxjava2/rxjava/2.2.21:rxjava-2.2.21:jar \
repo1.maven.org/maven2/io/reactivex/rxjava2/rxandroid/2.1.1:rxandroid-2.1.1:aar \
repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.4:reactive-streams-1.0.4:jar
JDK_DOWNLOAD_URI = https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip
JDK_DIR = $(ROOT_DIR)java\jdk-17.0.2^\
ADK_CMD_DOWNLOAD_URI = https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip
# We use ADK_DIR instead of ANDROID_HOME to distinguish this project's copy of the Android tools vs. any global copy that exists.
ADK_DIR = $(ROOT_DIR)android^\
BUILD_TOOLS_DIR = $(ADK_DIR)build-tools\$(ANDROID_TARGET_VERSION).0.0^\
PLATFORM_TOOLS_DIR = $(ADK_DIR)platform-tools^\
PLATFORM_DIR = $(ADK_DIR)platforms\android-$(ANDROID_TARGET_VERSION)^\

# Real targets.
# Default/build target only compiles and links, but does not run.
_default: build

# Download and extract JDK.
$(JDK_DIR).make:
	@IF EXIST $(ROOT_DIR)java RD /Q /S $(ROOT_DIR)java
	@MD $(ROOT_DIR)java
	@ECHO Downloading $(JDK_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(JDK_DOWNLOAD_URI)" \
		$(ROOT_DIR)java\jdk.zip
	@ECHO Extracting to $(JDK_DIR)...
	@TAR -xf $(ROOT_DIR)java\jdk.zip -C $(ROOT_DIR)java
	@DEL $(ROOT_DIR)java\jdk.zip
	@COPY NUL $(JDK_DIR).make

# Download and extract all relevant Android development tools.
$(ADK_DIR).make: $(JDK_DIR).make
	@IF EXIST $(ROOT_DIR)android RD /Q /S $(ROOT_DIR)android
	@MD $(ROOT_DIR)android
	@ECHO Downloading $(ADK_CMD_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(ADK_CMD_DOWNLOAD_URI)" \
		$(ROOT_DIR)android\adk-cmd.zip
	@ECHO Extracting to $(ADK_DIR)...
	@TAR -xf $(ROOT_DIR)android\adk-cmd.zip -C $(ROOT_DIR)android
	@DEL $(ROOT_DIR)android\adk-cmd.zip
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager" \
		--sdk_root=$(ADK_DIR) --licenses && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager" \
		--sdk_root=$(ADK_DIR) --install \
		"platforms;android-$(ANDROID_TARGET_VERSION)" \
		"build-tools;$(ANDROID_TARGET_VERSION).0.0" \
		"platform-tools" \
		"emulator" \
		"system-images;android-$(ANDROID_MINIMUM_VERSION);google_apis;x86_64" && \
	"$(ADK_DIR)cmdline-tools\bin\avdmanager" create avd \
		--name $(ANDROID_MINIMUM_VERSION)-x86_64 \
		--force --package \
		"system-images;android-$(ANDROID_MINIMUM_VERSION);google_apis;x86_64"
	@COPY NUL $(ADK_DIR).make

# Use the provided tools.
adb: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(PLATFORM_TOOLS_DIR)adb" $(ARGS)

sdkmanager: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager" --sdk_root=$(ADK_DIR) $(ARGS)

avdmanager: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\avdmanager" $(ARGS)

# Ensure that ~/.android/avd/<avd-name>/config.ini has the correct `image.
# sysdir.1` relative property.
emulator: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)emulator\emulator" $(ARGS)

# All libraries are made together. The last file is specified as target to track
# dependency, since tracking a directory is not possible.
$(ROOT_DIR)lib\.make: $(JDK_DIR).make
	@IF EXIST $(ROOT_DIR)lib RD /Q /S $(ROOT_DIR)lib
	@MD $(ROOT_DIR)lib
	@FOR %%I IN ($(LIB_NAMES)) DO @( \
		@FOR /F "tokens=1-3 delims=:" %%J IN ("%%I") DO @( \
			@ECHO Downloading https://%%J/%%K.%%L... && \
			BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
				/DOWNLOAD /PRIORITY FOREGROUND \
				"https://%%J/%%K.%%L" \
				"$(ROOT_DIR)lib\%%K.%%L" && \
			IF "%%L" == "aar" ( \
				@ECHO Extracting to $(ROOT_DIR)lib\%%K.jar... && \
				CALL "$(JDK_DIR)bin\jar" xf $(ROOT_DIR)lib\%%K.aar classes.jar && \
				MOVE classes.jar $(ROOT_DIR)lib\%%K.jar >NUL && \
				DEL $(ROOT_DIR)lib\%%K.aar \
			) \
		) \
	)
	@COPY NUL $(ROOT_DIR)lib\.make

# Create R.java.
$(ROOT_DIR)src\$(PACKAGE_PATH)\R.java: $(ADK_DIR).make $(ROOT_DIR)res\layout\* $(ROOT_DIR)res\values\* $(ROOT_DIR)res\drawable\*
	@ECHO Creating $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java...
	@IF NOT EXIST $(ROOT_DIR)obj MD $(ROOT_DIR)obj
	@CALL $(BUILD_TOOLS_DIR)aapt.exe package -f -m \
		-S "$(ROOT_DIR)res" \
		-J "$(ROOT_DIR)src" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-I $(PLATFORM_DIR)android.jar

$(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore: $(JDK_DIR).make
	@ECHO Creating $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore...
	@CALL "$(JDK_DIR)bin\keytool" -genkey -validity 10000 \
		-dname "CN=AndroidDebug, O=Android, C=US" \
		-keystore $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore \
		-storepass android -keypass android \
		-alias android_debug_key -keyalg RSA -keysize 2048

# Compile target: .java to .class.
$(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class: $(JDK_DIR).make $(ROOT_DIR)lib\.make $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java $(ROOT_DIR)src\$(PACKAGE_PATH)\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\filesystem\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\scribble\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\pdf\*.java
	@ECHO Compiling to .class...
	@DIR /A-D /S /B $(ROOT_DIR)src\*.java > src.tmp
	@CALL "$(JDK_DIR)bin\javac" \
		-source $(JAVA_SOURCE_VERSION) \
		-target $(JAVA_SOURCE_VERSION) \
		-d "$(ROOT_DIR)obj" \
		-cp "$(PLATFORM_DIR)android.jar";"$(ROOT_DIR)lib\*" \
		-sourcepath "$(ROOT_DIR)src" \
		-Xlint:deprecation \
		@src.tmp
	@DEL src.tmp

# Build target. R.class is used as dependency alias for compilation.
$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk: $(ADK_DIR).make $(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class $(ROOT_DIR)priv\$(PROJECT_NAME).$(BUILD_NAME).keystore $(ROOT_DIR)AndroidManifest.xml
	@ECHO Building to $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk...
	@IF NOT EXIST $(ROOT_DIR)bin MD $(ROOT_DIR)bin
# Generate APK.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && $(BUILD_TOOLS_DIR)aapt.exe package -f -m \
		-F "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-S "$(ROOT_DIR)res" \
		-I $(PLATFORM_DIR)android.jar
# Convert to DEX.
	@DIR /A-D /S /B $(ROOT_DIR)obj\*.class > obj.tmp
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && $(BUILD_TOOLS_DIR)d8.bat \
		--lib "$(PLATFORM_DIR)android.jar" \
		--min-api $(ANDROID_TARGET_VERSION) \
		--$(BUILD_NAME) \
		--output $(ROOT_DIR)bin \
		@obj.tmp \
		$(ROOT_DIR)lib\*.jar
	@DEL obj.tmp
# For some reason, aapt add only works for classes.dex in the current directory
	@MOVE "$(ROOT_DIR)bin\classes.dex" . >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && $(BUILD_TOOLS_DIR)aapt.exe add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		classes.dex
	@DEL classes.dex
# Add shared dependencies.
	@MKDIR lib lib\arm64-v8a lib\x86_64
	@COPY "$(ROOT_DIR)so\arm64-v8a" lib\arm64-v8a >NUL
	@COPY "$(ROOT_DIR)so\x86_64" lib\x86_64 >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && $(BUILD_TOOLS_DIR)aapt.exe add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		lib/arm64-v8a/libonyx_pen_touch_reader.so \
		lib/arm64-v8a/libc++_shared.so \
		lib/x86_64/libc++_shared.so
	@RD /Q /S lib
# Align to 4-byte boundaries.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && $(BUILD_TOOLS_DIR)zipalign.exe -f 4 \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk"
# Sign package.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)apksigner.bat" sign \
		--ks $(ROOT_DIR)priv\$(PROJECT_NAME).$(BUILD_NAME).keystore \
		--ks-pass pass:android \
		--ks-key-alias android_debug_key \
		--key-pass pass:android \
		--out $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk \
		$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"

build: $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk

# Build and install. App may not run if timings are off.
run: $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk
	@ECHO Uninstalling from device...
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe uninstall $(PACKAGE_NAME) &
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe install -r $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk
	@ECHO Cooling down after install...
	@TIMEOUT /T 2 >NUL
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe shell am start -n \
		$(PACKAGE_NAME)/$(PACKAGE_NAME).$(MAIN_CLASS)

# Clean target removes all intermediates.
shallow-clean:
	@IF EXIST $(ROOT_DIR)bin RD /Q /S $(ROOT_DIR)bin
	@IF EXIST $(ROOT_DIR)build/lib RD /Q /S $(ROOT_DIR)build/lib
	@IF EXIST $(ROOT_DIR)obj RD /Q /S $(ROOT_DIR)obj
	@IF EXIST $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore \
		DEL $(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore
	@IF EXIST $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java \
		DEL $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java
	@ECHO Uninstalling from device...
	@IF EXIST $(PLATFORM_TOOLS_DIR)adb.exe \
		@CALL $(PLATFORM_TOOLS_DIR)adb.exe uninstall $(PACKAGE_NAME) &
	@IF EXIST $(PLATFORM_TOOLS_DIR)adb.exe \
		@CALL $(PLATFORM_TOOLS_DIR)adb.exe kill-server &

clean: shallow-clean
	@IF EXIST $(ROOT_DIR)android RD /Q /S $(ROOT_DIR)android
	@IF EXIST $(ROOT_DIR)java RD /Q /S $(ROOT_DIR)java
	@IF EXIST $(ROOT_DIR)lib RD /Q /S $(ROOT_DIR)lib

logcat:
	@CALL $(PLATFORM_TOOLS_DIR)adb.exe shell logcat \
		AndroidRuntime:I ActivityManager:I Xena:V *:S

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
