# GNU make (gmake) & nmake shared makefile.
# Conditional logic implemented with differing end-of-line comment syntax.
# To enable multithreaded compilation, nmake compiles all files together with
# /MP, while gmake compiles separately with -jN.

# Relevant targets:
# clean
# logcat
# adb (ARGS=)
# emulator (ARGS=)
# build-apk (BUILD=0, 1)
# run-apk (BUILD=0, 1)
# build-exe (BUILD=0, 1, 2)
# run-exe (BUILD=0, 1, 2)

# Changes to track for Android build.
# 1. New .java source folders.
# 2. New .so libraries.
# 3. New res folders.

# Default target defined before any "fake" targets.
__default: _default

# ------------------------------------------------------------------------------
ifdef MAKEDIR: # gmake: false; nmake: unused target
!ifdef MAKEDIR # gmake: not seen; nmake: true
# ------------------------------------------------------------------------------

# nmake specific commands.
PROJECT_NAME = xena

ROOT_DIR = $(MAKEDIR)\..^\
OBJ_DIR = $(ROOT_DIR)obj^\
BIN_DIR = $(ROOT_DIR)bin^\
!IFNDEF PROJECT_DIR_NAME
PROJECT_DIR_NAME = src
!ENDIF
PROJECT_OBJ_DIR = $(OBJ_DIR)$(PROJECT_DIR_NAME)^\
PROJECT_SRC_DIR = $(ROOT_DIR)$(PROJECT_DIR_NAME)^\
!IFNDEF SOURCES_CPP
SOURCES_CPP = *.cpp
!ENDIF
!IFNDEF BIN
# For the repository, executable filename is not the project name.
BIN = $(PROJECT_NAME)
!ENDIF
PROJECT_BIN = $(BIN_DIR)$(BIN).exe
PROJECT_BIN_DIR = $(BIN_DIR)$(BIN)\..^\

!IFNDEF JAVA_SOURCE_VERSION
JAVA_SOURCE_VERSION = 17
!ENDIF
!IFNDEF ADK_VERSION
ADK_VERSION = 13114758
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
AAPT_BUILD_FLAGS =
!ELSE
BUILD_NAME = debug
AAPT_BUILD_FLAGS = --debug-mode
!ENDIF
NAMESPACE_PATH = com\gilgamesh
PACKAGE_PATH = $(NAMESPACE_PATH)\$(PROJECT_NAME)
PACKAGE_NAME = com.gilgamesh.$(PROJECT_NAME)
MAIN_CLASS = filesystem.FilePickerActivity

# All library files to be declared here.
LIB_NAMES = \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-pen/1.4.11:onyxsdk-pen-1.4.11:aar \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-base/1.7.6:onyxsdk-base-1.7.6:aar \
repo.boox.com/repository/maven-public/com/onyx/android/sdk/onyxsdk-device/1.2.30:onyxsdk-device-1.2.30:aar \
repo1.maven.org/maven2/io/reactivex/rxjava2/rxjava/2.2.21:rxjava-2.2.21:jar \
repo1.maven.org/maven2/io/reactivex/rxjava2/rxandroid/2.1.1:rxandroid-2.1.1:aar \
maven.google.com/androidx/preference/preference/1.2.1:preference-1.2.1:aar \
maven.google.com/androidx/appcompat/appcompat/1.1.0:appcompat-1.1.0:aar \
maven.google.com/androidx/activity/activity/1.0.0:activity-1.0.0:aar \
maven.google.com/androidx/core/core/1.6.0:core-1.6.0:aar \
maven.google.com/androidx/fragment/fragment/1.1.0:fragment-1.1.0:aar \
maven.google.com/androidx/annotation/annotation/1.2.0:annotation-1.2.0:jar \
repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.4:reactive-streams-1.0.4:jar
DEV_DIR = $(ROOT_DIR)dev^\
JDK_DOWNLOAD_URI = https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip
JDK_DIR = $(DEV_DIR)jdk-17.0.2^\
ADK_CMD_DOWNLOAD_URI = https://dl.google.com/android/repository/commandlinetools-win-$(ADK_VERSION)_latest.zip
# We use ADK_DIR instead of ANDROID_HOME to distinguish this project's copy of the Android tools vs. any global copy that exists.
ADK_DIR = $(DEV_DIR)adk-$(ADK_VERSION)^\
BUILD_TOOLS_DIR = $(ADK_DIR)build-tools\$(ANDROID_TARGET_VERSION).0.0^\
PLATFORM_TOOLS_DIR = $(ADK_DIR)platform-tools^\
PLATFORM_DIR = $(ADK_DIR)platforms\android-$(ANDROID_TARGET_VERSION)^\
DEV_LIB_DIR = $(DEV_DIR)lib^\

# CPP settings.
PROJECT_INCLUDE_DIR = $(ROOT_DIR)include^\
PROJECT_VERSION_INCLUDE = $(PROJECT_INCLUDE_DIR)build.hpp
# TODO: This macro should include all header files under PROJECT_INCLUDE_DIR.
PROJECT_INCLUDES =
# TODO: This macro should include all header files in Rain.
RAIN_INCLUDES =

COMPILER = cl /c
COMPILER_FLAGS_INCLUDES = /I$(PROJECT_INCLUDE_DIR) /I$(ROOT_DIR)rain\include^\
COMPILER_FLAGS_COMMON = $(COMPILER_FLAGS_INCLUDES) /std:c++17 /D _CONSOLE\
	/Fd$(PROJECT_OBJ_DIR).pdb /Fo$(PROJECT_OBJ_DIR) /Fp$(PROJECT_OBJ_DIR).pch\
	/fp:fast /MP /permissive- /Zc:wchar_t /Zc:forScope /Zc:inline /Zf\
	/GS /W3 /WX- /wd4250 /sdl /diagnostics:column /EHsc /Gm- /nologo
COMPILER_FLAGS_DEBUG = /D _DEBUG /MDd /Od /RTC1 /JMC /ZI
COMPILER_FLAGS_RELEASE = /D NDEBUG /MT /O2 /Oi /GL /Gy /Zi
COMPILER_FLAGS_INSTRUMENT = $(COMPILER_FLAGS_DEBUG) /Fa$(PROJECT_OBJ_DIR)

LINKER = link
LINKER_FLAGS_LIBRARIES =
LINKER_FLAGS_COMMON = $(LINKER_FLAGS_LIBRARIES) /OUT:$(PROJECT_BIN)\
	/PDB:$(PROJECT_OBJ_DIR).pdb /ILK:$(PROJECT_OBJ_DIR).ilk\
	/MANIFESTUAC:"level='asInvoker' uiAccess='false'"\
	/MANIFESTFILE:$(PROJECT_OBJ_DIR).exe.intermediate.manifest\
	/LTCGOUT:$(PROJECT_OBJ_DIR).iobj /SUBSYSTEM:CONSOLE /NOLOGO
LINKER_FLAGS_DEBUG = /DEBUG
LINKER_FLAGS_RELEASE = /INCREMENTAL:NO /OPT:ICF /OPT:REF /LTCG:incremental
LINKER_FLAGS_INSTRUMENT = $(LINKER_FLAGS_DEBUG)

!IF $(BUILD) == 1
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_RELEASE)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_RELEASE)
!ELSE IF $(BUILD) == 2
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_INSTRUMENT)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_INSTRUMENT)
!ELSE
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_DEBUG)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_DEBUG)
!ENDIF

# Real targets.
# Default/build target only compiles and links, but does not run.
_default: build-apk

# Increment version number.
$(PROJECT_VERSION_INCLUDE): $(PROJECT_INCLUDES) $(PROJECT_SRC_DIR)$(SOURCES_CPP) $(RAIN_INCLUDES)
	@<<increment.tmp.bat
@ECHO OFF
@REM Increments build version number at compile-time.

@REM LF newline variable.
(SET \n=^

)

SETLOCAL ENABLEDELAYEDEXPANSION
SET FILE_PATH=%~dp0..\include\build.hpp
FOR /F "tokens=1-3 delims= " %%A IN (%FILE_PATH%) DO (
	SET /A BUILD=%%C+1

	REM ECHO with custom LF-only newline.
	<NUL SET /P=%%A %%B !BUILD!!\n!> %FILE_PATH%
)
ECHO VERSION_BUILD: !BUILD!.

@REM TODO: copy to Android resources.
ENDLOCAL
<<NOKEEP

# Download and extract JDK.
$(JDK_DIR).make:
	@IF EXIST $(DEV_DIR) RD /Q /S $(DEV_DIR)
	@MD $(DEV_DIR)
	@ECHO Downloading $(JDK_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(JDK_DOWNLOAD_URI)" \
		$(DEV_DIR)jdk.zip
	@ECHO Extracting to $(JDK_DIR)...
	@TAR -xf $(DEV_DIR)jdk.zip -C $(DEV_DIR)
	@DEL $(DEV_DIR)jdk.zip
	@COPY NUL $(JDK_DIR).make

# Download and extract all relevant Android development tools.
$(ADK_DIR).make: $(JDK_DIR).make
	@IF EXIST $(ADK_DIR) RD /Q /S $(ADK_DIR)
	@MD $(ADK_DIR)
	@ECHO Downloading $(ADK_CMD_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(ADK_CMD_DOWNLOAD_URI)" \
		$(ADK_DIR)\adk.zip
	@ECHO Extracting to $(ADK_DIR)...
	@TAR -xf $(ADK_DIR)adk.zip -C $(ADK_DIR)
	@DEL $(ADK_DIR)adk.zip
# Attempt to automatically accept all licenses.
	@<<adk.tmp.bat
@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
@REM LF newline variable.
(SET \n=^

)
ECHO y!\n!y!\n!y!\n!y!\n!y!\n!y!\n!y!\n! > yes.tmp.txt
ECHO no!\n! > no.tmp.txt
ENDLOCAL
<<NOKEEP
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager.bat" \
		--sdk_root=$(ADK_DIR) --licenses < yes.tmp.txt
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager.bat" \
		--sdk_root=$(ADK_DIR) --install \
		"platforms;android-$(ANDROID_TARGET_VERSION)" \
		"build-tools;$(ANDROID_TARGET_VERSION).0.0" \
		"platform-tools" \
		"emulator" \
		"system-images;android-$(ANDROID_MINIMUM_VERSION);google_apis;x86_64" && \
	"$(ADK_DIR)cmdline-tools\bin\avdmanager.bat" create avd \
		--name $(ANDROID_MINIMUM_VERSION)-x86_64 \
		--force --package \
		"system-images;android-$(ANDROID_MINIMUM_VERSION);google_apis;x86_64" < no.tmp.txt
	@DEL yes.tmp.txt no.tmp.txt
	@COPY NUL $(ADK_DIR).make

# Shortcuts for Android tools.
adb: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(PLATFORM_TOOLS_DIR)adb.exe" $(ARGS)

# Ensure that ~/.android/avd/<avd-name>/config.ini has the correct `image.
# sysdir.1` relative property. Density (DPI) should probably be increased as
# well.
emulator: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)emulator\emulator" $(ARGS)

# All libraries are made together.
$(DEV_LIB_DIR).make: $(JDK_DIR).make
	@IF EXIST $(DEV_LIB_DIR) RD /Q /S $(DEV_LIB_DIR)
	@MD $(DEV_LIB_DIR)
	@FOR %%I IN ($(LIB_NAMES)) DO @( \
		@FOR /F "tokens=1-3 delims=:" %%J IN ("%%I") DO @( \
			@ECHO Downloading https://%%J/%%K.%%L... && \
			BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
				/DOWNLOAD /PRIORITY FOREGROUND \
				"https://%%J/%%K.%%L" \
				"$(DEV_LIB_DIR)%%K.%%L" && \
			IF "%%L" == "aar" ( \
				@ECHO Extracting to $(DEV_LIB_DIR)%%K.jar... && \
				CALL "$(JDK_DIR)bin\jar" xf $(DEV_LIB_DIR)%%K.aar classes.jar && \
				MOVE classes.jar $(DEV_LIB_DIR)%%K.jar >NUL && \
				DEL $(DEV_LIB_DIR)%%K.aar \
			) \
		) \
	)
	@COPY NUL $(DEV_LIB_DIR).make

# Create R.java.
$(ROOT_DIR)src\$(PACKAGE_PATH)\R.java: $(ADK_DIR).make $(ROOT_DIR)res\layout\* $(ROOT_DIR)res\values\* $(ROOT_DIR)res\drawable\*
	@ECHO Creating $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java...
	@IF NOT EXIST $(ROOT_DIR)obj MD $(ROOT_DIR)obj
	@CALL $(BUILD_TOOLS_DIR)aapt.exe package -f -m \
		$(AAPT_BUILD_FLAGS) \
		-S "$(ROOT_DIR)res" \
		-J "$(ROOT_DIR)src" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-I $(PLATFORM_DIR)android.jar

$(ROOT_DIR)priv\.make: $(JDK_DIR).make
	@ECHO Creating $(ROOT_DIR)priv\$(PROJECT_NAME).*.keystore...
	@IF EXIST $(ROOT_DIR)priv RD /Q /S $(ROOT_DIR)priv
	@MD $(ROOT_DIR)priv
	@CALL "$(JDK_DIR)bin\keytool.exe" -genkey -validity 10000 \
		-dname "CN=AndroidDebug, O=Android, C=US" \
		-keystore "$(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore" \
		-storepass android -keypass android \
		-alias "$(PROJECT_NAME)_key" -keyalg RSA -keysize 2048
	@CP "$(ROOT_DIR)priv\$(PROJECT_NAME).debug.keystore" \
		"$(ROOT_DIR)priv\$(PROJECT_NAME).release.keystore"
	@COPY NUL $(ROOT_DIR)priv\.make

# Compile target: .java to .class.
$(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class: $(JDK_DIR).make $(DEV_LIB_DIR).make $(ROOT_DIR)src\$(NAMESPACE_PATH)\algorithm\*.java $(ROOT_DIR)src\$(NAMESPACE_PATH)\multithreading\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\R.java $(ROOT_DIR)src\$(PACKAGE_PATH)\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\filesystem\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\pdf\*.java $(ROOT_DIR)src\$(PACKAGE_PATH)\scribble\*.java
	@ECHO Compiling to .class...
	@DIR /A-D /S /B $(ROOT_DIR)src\*.java > src.tmp
	@CALL "$(JDK_DIR)bin\javac" \
		-source $(JAVA_SOURCE_VERSION) \
		-target $(JAVA_SOURCE_VERSION) \
		-d "$(ROOT_DIR)obj" \
		-cp "$(PLATFORM_DIR)android.jar";"$(DEV_LIB_DIR)*" \
		-sourcepath "$(ROOT_DIR)src" \
		-Xlint:deprecation \
		@src.tmp
	@DEL src.tmp

# Build target. R.class is used as dependency alias for compilation.
$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk: $(ADK_DIR).make $(ROOT_DIR)obj\$(PACKAGE_PATH)\R.class $(ROOT_DIR)priv\.make $(ROOT_DIR)AndroidManifest.xml
	@ECHO Building to $(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk...
	@IF NOT EXIST $(ROOT_DIR)bin MD $(ROOT_DIR)bin
# Generate APK.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)aapt.exe" package \
		-f -m \
		$(AAPT_BUILD_FLAGS) \
		-F "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-S "$(ROOT_DIR)res" \
		-I "$(PLATFORM_DIR)android.jar"
# Convert to DEX.
	@DIR /A-D /S /B $(ROOT_DIR)obj\*.class > obj.tmp
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)d8.bat" \
		--lib "$(PLATFORM_DIR)android.jar" \
		--min-api $(ANDROID_TARGET_VERSION) \
		--$(BUILD_NAME) \
		--output "$(ROOT_DIR)bin" \
		"@obj.tmp" \
		$(DEV_LIB_DIR)*.jar
	@DEL obj.tmp
# For some reason, aapt add only works for classes.dex in the current directory.
	@MOVE "$(ROOT_DIR)bin\classes.dex" . >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)aapt.exe" add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		classes.dex
	@DEL classes.dex
# Add shared dependencies.
	@MKDIR lib lib\arm64-v8a lib\x86_64
	@COPY "$(ROOT_DIR)lib\arm64-v8a" lib\arm64-v8a >NUL
	@COPY "$(ROOT_DIR)lib\x86_64" lib\x86_64 >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)aapt.exe" add \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		lib/arm64-v8a/libonyx_pen_touch_reader.so \
		lib/arm64-v8a/libc++_shared.so \
		lib/x86_64/libc++_shared.so
	@RD /Q /S lib
# Align to 4-byte boundaries.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)zipalign.exe" -f 4 \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
	"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.unaligned.apk"
# Sign package.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(BUILD_TOOLS_DIR)apksigner.bat" sign \
		--ks "$(ROOT_DIR)priv\$(PROJECT_NAME).$(BUILD_NAME).keystore" \
		--ks-pass pass:android \
		--ks-key-alias "$(PROJECT_NAME)_key" \
		--key-pass pass:android \
		--out "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk" \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).unsigned.apk"

build-apk: "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk"

# Build and install. App may not run if timings are off.
run-apk: "$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk"
	@CALL "$(PLATFORM_TOOLS_DIR)adb.exe" install \
		"$(ROOT_DIR)bin\$(PROJECT_NAME).$(BUILD_NAME).apk"
	@ECHO Cooling down after install...
	@TIMEOUT /T 2 >NUL
	@CALL "$(PLATFORM_TOOLS_DIR)adb.exe" shell am start -n \
		"$(PACKAGE_NAME)/$(PACKAGE_NAME).$(MAIN_CLASS)""

# Clean target removes all intermediates.
shallow-clean:
	@IF EXIST "$(BIN_DIR)" RD /Q /S "$(BIN_DIR)"
	@IF EXIST "$(OBJ_DIR)" RD /Q /S "$(OBJ_DIR)"
	@IF EXIST "$(ROOT_DIR)build/lib" RD /Q /S "$(ROOT_DIR)build/lib"
	@IF EXIST "$(ROOT_DIR)src\$(PACKAGE_PATH)\R.java" \
		DEL "$(ROOT_DIR)src\$(PACKAGE_PATH)\R.java"
	@ECHO Uninstalling from device...
	@IF EXIST "$(PLATFORM_TOOLS_DIR)adb.exe" \
		@CALL "$(PLATFORM_TOOLS_DIR)adb.exe" uninstall $(PACKAGE_NAME) &
	@IF EXIST "$(PLATFORM_TOOLS_DIR)adb.exe" \
		@CALL "$(PLATFORM_TOOLS_DIR)adb.exe" kill-server &

logcat:
	@CALL "$(PLATFORM_TOOLS_DIR)adb.exe" shell logcat \
		AndroidRuntime:I ActivityManager:I Xena:V *:S

build-exe: $(PROJECT_VERSION_INCLUDE) $(PROJECT_BIN)
# Build and run.
run-exe: build-exe
	@CD $(BIN_DIR) && $(PROJECT_BIN) $(ARGUMENTS)
# Clean target removes all intermediates.

# Define special macro to enable double-substitution.
DBLSUB_DEP_SRC_DIR_TO_OBJ_DIR = $(**:..\=..\obj\)
# First compile all outdated files (with /MP), then link everything together.
# TODO: Dependencies are parsed incorrectly once PROJECT_INCLUDES is set.
$(PROJECT_BIN): $(PROJECT_SRC_DIR)$(SOURCES_CPP) $(PROJECT_INCLUDES) $(RAIN_INCLUDES)
	@IF NOT EXIST $(PROJECT_BIN_DIR) MD $(PROJECT_BIN_DIR)
	@IF NOT EXIST $(PROJECT_OBJ_DIR) MD $(PROJECT_OBJ_DIR)
	@$(COMPILER) $(COMPILER_FLAGS) $?
	@ECHO $(PROJECT_BIN)
	@$(LINKER) $(LINKER_FLAGS) $(DBLSUB_DEP_SRC_DIR_TO_OBJ_DIR:.cpp=.obj)

clean: shallow-clean
	@IF EXIST "$(DEV_DIR)" RD /Q /S "$(DEV_DIR)"

# ------------------------------------------------------------------------------
!else
else
# ------------------------------------------------------------------------------

# GNU make (gmake) specific commands.

# Ensure files with the same name as fake targets do not trigger.
.PHONY: __default _default
_default: 
	@echo "`gmake` on Linux not supported. Use `nmake` on Windows instead.""

# ------------------------------------------------------------------------------
endif    # gmake: close condition; nmake: not seen
!endif : # gmake: unused target; nmake close conditional
# ------------------------------------------------------------------------------
