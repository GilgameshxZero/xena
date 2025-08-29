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

# nmake-specific commands.
!IFNDEF JDK_VERSION
JDK_VERSION = 17
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

ROOT_DIR = $(MAKEDIR)\..^\
REPO_NAME = xena
APK_NAMESPACE_PATH = com\gilgamesh
APK_PACKAGE_PATH = $(APK_NAMESPACE_PATH)\$(REPO_NAME)
APK_PACKAGE_NAME = com.gilgamesh.$(REPO_NAME)
APK_MAIN_CLASS = filesystem.FilePickerActivity
!IFNDEF PROJ
PROJ = src
!ENDIF
PROJ_NAME = $(PROJ)
# For this repo, BIN and SRC should not be used.
!IFNDEF BIN
!IFNDEF SRC
BIN = $(REPO_NAME)
# This will include all *.cpp* directories as well. In this case, name
# directories as *.cpp.assets.
SRC = *.cpp
!ELSE
BIN = $(patsubsti %.cpp,%,$(SRC))
!ENDIF
!ELSE
!IFNDEF SRC
SRC = $(BIN).cpp
!ENDIF
!ENDIF
BIN_NAME = $(BIN)
SRC_NAME = $(SRC)
# Some sources must be manually specified here.
PROJ_SRC_JAVA = $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\BaseActivity.java $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\XenaApplication.java $(ROOT_DIR)src\$(APK_NAMESPACE_PATH)\algorithm\*.java $(ROOT_DIR)src\$(APK_NAMESPACE_PATH)\multithreading\*.java $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\filesystem\*.java $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\pdf\*.java $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\scribble\*.java
PROJ_SRC_RES = $(ROOT_DIR)res\drawable\* $(ROOT_DIR)res\layout\* $(ROOT_DIR)res\values\*
!IFNDEF BUILD
BUILD = 0
!ENDIF
!IF $(BUILD) == 2
BUILD_NAME = instrument
!ELSE IF $(BUILD) == 1
BUILD_NAME = release
!ELSE
BUILD_NAME = debug
!ENDIF
!IFNDEF ARCH
ARCH = 64
!ENDIF
!IFNDEF PCH
PCH = 1
!ENDIF

BIN_DIR = $(ROOT_DIR).bin^\
PROJ_BIN = $(BIN_DIR)$(PROJ_NAME)\$(BIN_NAME).$(BUILD_NAME).x$(ARCH).exe
PROJ_BIN_DIR = $(BIN_DIR)$(PROJ_NAME)^\
OBJ_DIR = $(ROOT_DIR).obj^\
PROJ_OBJ_DIR = $(OBJ_DIR)$(PROJ_NAME)^\
PROJ_SRC_DIR = $(ROOT_DIR)$(PROJ_NAME)^\
PROJ_SRC = $(patsubsti %,$(PROJ_SRC_DIR)\%,$(SRC_NAME))
DATA_DIR = $(ROOT_DIR).data^\
LIB_DIR = $(DATA_DIR)lib^\
JDK_DIR = $(DATA_DIR)jdk-17.0.2^\
# We use ADK_DIR instead of ANDROID_HOME to distinguish this project's copy of the Android tools vs. any global copy that exists.
ADK_DIR = $(DATA_DIR)adk-$(ADK_VERSION)^\
ADK_BUILD_TOOLS_DIR = $(ADK_DIR)build-tools\$(ANDROID_TARGET_VERSION).0.0^\
ADK_PLATFORM_TOOLS_DIR = $(ADK_DIR)platform-tools^\
ADK_PLATFORMS_DIR = $(ADK_DIR)platforms\android-$(ANDROID_TARGET_VERSION)^\

# All library files to be declared here.
JDK_DOWNLOAD_URI = https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip
ADK_DOWNLOAD_URI = https://dl.google.com/android/repository/commandlinetools-win-$(ADK_VERSION)_latest.zip
LIB_NAME = \
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

# CPP settings.
COMPILER = cl /c
COMPILER_FLAGS_INCLUDES = /I $(ROOT_DIR)include /I $(ROOT_DIR)rain\include
COMPILER_FLAGS_COMMON = $(COMPILER_FLAGS_INCLUDES) /std:c++20 /D _CONSOLE\
	/Fp$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pch\
	/fp:fast /MP /permissive-\
	/Zc:__cplusplus /Zc:wchar_t /Zc:forScope /Zc:inline /Zf\
	/GS /W3 /WX- /wd4250 /wd4068 /sdl /diagnostics:column /EHsc /Gm- /nologo
COMPILER_FLAGS_DEBUG = /D _DEBUG /MDd /Od /RTC1 /JMC /ZI
COMPILER_FLAGS_RELEASE = /D NDEBUG /MT /O2 /Oi /GL /Gy /Zi
COMPILER_FLAGS_INSTRUMENT = $(COMPILER_FLAGS_DEBUG) /Fa$(PROJ_OBJ_DIR)
COMPILER_FLAGS_64 =
COMPILER_FLAGS_86 =

LINKER = link
LINKER_FLAGS_LIBRARIES =
LINKER_FLAGS_COMMON = $(LINKER_FLAGS_LIBRARIES) /OUT:$(PROJ_BIN)\
	/PDB:$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).pdb\
	/ILK:$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).ilk\
	/MANIFESTUAC:"level='asInvoker' uiAccess='false'"\
	/MANIFESTFILE:$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).exe.intermediate.manifest\
	/LTCGOUT:$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).iobj\
	/SUBSYSTEM:CONSOLE /NOLOGO
LINKER_FLAGS_DEBUG = /DEBUG
LINKER_FLAGS_RELEASE = /INCREMENTAL:NO /OPT:ICF /OPT:REF /LTCG:incremental
LINKER_FLAGS_INSTRUMENT = $(LINKER_FLAGS_DEBUG)
LINKER_FLAGS_64 = /MACHINE:x64
LINKER_FLAGS_86 = /MACHINE:x86

AAPT_BUILD_FLAGS =
AAPT_BUILD_FLAGS_DEBUG = --debug-mode
AAPT_BUILD_FLAGS_RELEASE =
AAPT_BUILD_FLAGS_INSTRUMENT =

!IF "$(BUILD_NAME)" == "instrument"
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_INSTRUMENT)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_INSTRUMENT)
AAPT_BUILD_FLAGS = $(AAPT_BUILD_FLAGS) $(AAPT_BUILD_FLAGS_INSTRUMENT)
!ELSE IF "$(BUILD_NAME)" == "release"
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_RELEASE)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_RELEASE)
AAPT_BUILD_FLAGS = $(AAPT_BUILD_FLAGS) $(AAPT_BUILD_FLAGS_RELEASE)
!ELSE
COMPILER_FLAGS = $(COMPILER_FLAGS_COMMON) $(COMPILER_FLAGS_DEBUG)
LINKER_FLAGS = $(LINKER_FLAGS_COMMON) $(LINKER_FLAGS_DEBUG)
AAPT_BUILD_FLAGS = $(AAPT_BUILD_FLAGS) $(AAPT_BUILD_FLAGS_DEBUG)
!ENDIF

!IF $(ARCH) == 86
COMPILER_FLAGS = $(COMPILER_FLAGS) $(COMPILER_FLAGS_86)
LINKER_FLAGS = $(LINKER_FLAGS) $(LINKER_FLAGS_86)
!ELSE
COMPILER_FLAGS = $(COMPILER_FLAGS) $(COMPILER_FLAGS_64)
LINKER_FLAGS = $(LINKER_FLAGS) $(LINKER_FLAGS_64)
!ENDIF

# Real targets.
# Default/build target only compiles and links, but does not run.
_default: build
build: build-apk build-exe
run: run-apk run-exe
build-apk: version.build.txt "$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).apk"
# Build and install. App may not run if timings are off.
run-apk: "$(ROOT_DIR)bin\$(REPO_NAME).$(BUILD_NAME).apk"
	@CALL "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" install \
		"$(ROOT_DIR)bin\$(REPO_NAME).$(BUILD_NAME).apk"
	@ECHO Cooling down after install...
	@TIMEOUT /T 2 >NUL
	@CALL "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" shell am start -n \
		"$(APK_PACKAGE_NAME)/$(APK_PACKAGE_NAME).$(APK_MAIN_CLASS)"
logcat:
	@CALL "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" shell logcat \
		AndroidRuntime:I ActivityManager:I Xena:V *:S
# Shortcuts for Android tools.
adb: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_PLATFORM_TOOLS_DIR)adb.exe" $(ARGS)
# Ensure that ~/.android/avd/<avd-name>/config.ini has the correct `image.
# sysdir.1` relative property. Density (DPI) should probably be increased as
# well.
emulator: $(ADK_DIR).make
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)emulator\emulator" $(ARGS)
build-exe: version.build.txt $(PROJ_BIN)
# Build and run.
run-exe: build-exe
	@ECHO ---------------- [Run] $(BIN_NAME) ----------------
	@CD $(BIN_DIR)$(PROJ_NAME) &&\
		$(BIN_NAME).$(BUILD_NAME).x$(ARCH).exe $(ARGS)
# Clean target removes all intermediates.
shallow-clean:
	@ECHO Cleaning...
	@IF EXIST "$(BIN_DIR)" RD /Q /S "$(BIN_DIR)"
	@IF EXIST "$(OBJ_DIR)" RD /Q /S "$(OBJ_DIR)"
	@IF EXIST "$(ROOT_DIR)build/lib" RD /Q /S "$(ROOT_DIR)build/lib"
	@IF EXIST "$(PROJ_SRC_DIR)$(APK_PACKAGE_PATH)\R.java" \
		DEL "$(PROJ_SRC_DIR)$(APK_PACKAGE_PATH)\R.java"
	@ECHO Uninstalling from device...
	@IF EXIST "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" \
		@CALL "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" uninstall $(APK_PACKAGE_NAME) &
	@IF EXIST "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" \
		@CALL "$(ADK_PLATFORM_TOOLS_DIR)adb.exe" kill-server &
clean: shallow-clean
	@IF EXIST "$(DATA_DIR)" RD /Q /S "$(DATA_DIR)"

# Increment version number.
version.build.txt: $(INCL) $(PROJ_SRC) $(PROJ_SRC_JAVA) $(PROJ_SRC_RES)
	@version.bat $(uppercase $(REPO_NAME))
	@$(MAKE) /C /T version.build.txt

# Download and extract JDK.
$(JDK_DIR).make:
	@IF EXIST $(DATA_DIR) RD /Q /S $(DATA_DIR)
	@MD $(DATA_DIR)
	@ECHO Downloading $(JDK_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(JDK_DOWNLOAD_URI)" \
		$(DATA_DIR)jdk.zip
	@ECHO Extracting to $(JDK_DIR)...
	@TAR -xf $(DATA_DIR)jdk.zip -C $(DATA_DIR)
	@DEL $(DATA_DIR)jdk.zip
	@COPY NUL $(JDK_DIR).make

# Download and extract all relevant Android development tools.
$(ADK_DIR).make: $(JDK_DIR).make
	@IF EXIST $(ADK_DIR) RD /Q /S $(ADK_DIR)
	@MD $(ADK_DIR)
	@ECHO Downloading $(ADK_DOWNLOAD_URI)...
	@BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
		/DOWNLOAD /PRIORITY FOREGROUND \
		"$(ADK_DOWNLOAD_URI)" \
		$(ADK_DIR)\adk.zip
	@ECHO Extracting to $(ADK_DIR)...
	@TAR -xf $(ADK_DIR)adk.zip -C $(ADK_DIR)
	@DEL $(ADK_DIR)adk.zip
# Attempt to automatically accept all licenses.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && \
	"$(ADK_DIR)cmdline-tools\bin\sdkmanager.bat" \
		--sdk_root=$(ADK_DIR) --licenses < y.txt
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
		"system-images;android-$(ANDROID_MINIMUM_VERSION);google_apis;x86_64" < no.txt
	@COPY NUL $(ADK_DIR).make

# All libraries are made together.
$(LIB_DIR).make: $(JDK_DIR).make
	@IF EXIST $(LIB_DIR) RD /Q /S $(LIB_DIR)
	@MD $(LIB_DIR)
	@FOR %%I IN ($(LIB_NAME)) DO @( \
		@FOR /F "tokens=1-3 delims=:" %%J IN ("%%I") DO @( \
			@ECHO Downloading https://%%J/%%K.%%L... && \
			BITSADMIN /RAWRETURN /TRANSFER LIB_DOWNLOAD \
				/DOWNLOAD /PRIORITY FOREGROUND \
				"https://%%J/%%K.%%L" \
				"$(LIB_DIR)%%K.%%L" && \
			IF "%%L" == "aar" ( \
				@ECHO Extracting to $(LIB_DIR)%%K.jar... && \
				CALL "$(JDK_DIR)bin\jar" xf $(LIB_DIR)%%K.aar classes.jar && \
				MOVE classes.jar $(LIB_DIR)%%K.jar >NUL && \
				DEL $(LIB_DIR)%%K.aar \
			) \
		) \
	)
	@COPY NUL $(LIB_DIR).make

# Create R.java.
$(ROOT_DIR)src\$(APK_PACKAGE_PATH)\R.java: $(ADK_DIR).make $(PROJ_SRC_RES)
	@ECHO Creating $(ROOT_DIR)src\$(APK_PACKAGE_PATH)\R.java...
	@IF NOT EXIST $(OBJ_DIR) MD $(OBJ_DIR)
	@CALL $(ADK_BUILD_TOOLS_DIR)aapt.exe package -f -m \
		$(AAPT_BUILD_FLAGS) \
		-S "$(ROOT_DIR)res" \
		-J "$(ROOT_DIR)src" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-I $(ADK_PLATFORMS_DIR)android.jar

# Create signing keys if necessary.
$(ROOT_DIR)priv\.make: $(JDK_DIR).make
	@ECHO Creating $(ROOT_DIR)priv\$(REPO_NAME).*.keystore...
	@IF EXIST $(ROOT_DIR)priv RD /Q /S $(ROOT_DIR)priv
	@MD $(ROOT_DIR)priv
	@CALL "$(JDK_DIR)bin\keytool.exe" -genkey -validity 10000 \
		-dname "CN=AndroidDebug, O=Android, C=US" \
		-keystore "$(ROOT_DIR)priv\$(REPO_NAME).debug.keystore" \
		-storepass android -keypass android \
		-alias "$(REPO_NAME)_key" -keyalg RSA -keysize 2048
	@CP "$(ROOT_DIR)priv\$(REPO_NAME).debug.keystore" \
		"$(ROOT_DIR)priv\$(REPO_NAME).release.keystore"
	@COPY NUL $(ROOT_DIR)priv\.make

# Compile target: .java to .class.
$(OBJ_DIR)$(APK_PACKAGE_PATH)\R.class: $(JDK_DIR).make $(LIB_DIR).make $(PROJ_SRC_DIR)$(APK_PACKAGE_PATH)\R.java $(PROJ_SRC_JAVA)
	@ECHO Compiling to .class...
	@DIR /A-D /S /B $(PROJ_SRC_DIR)*.java > src.tmp
	@CALL "$(JDK_DIR)bin\javac" \
		-source $(JDK_VERSION) \
		-target $(JDK_VERSION) \
		-d "$(ROOT_DIR)\.obj" \
		-cp "$(ADK_PLATFORMS_DIR)android.jar";"$(LIB_DIR)*" \
		-sourcepath "$(PROJ_SRC_DIR)*" \
		-Xlint:deprecation \
		@src.tmp
	@DEL src.tmp

# Build target. R.class is used as dependency alias for compilation.
$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).apk: $(ADK_DIR).make $(OBJ_DIR)$(APK_PACKAGE_PATH)\R.class $(ROOT_DIR)priv\.make $(ROOT_DIR)AndroidManifest.xml
	@ECHO Building to $(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).apk...
	@IF NOT EXIST $(BIN_DIR) MD $(BIN_DIR)
# Generate APK.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)aapt.exe" package \
		-f -m \
		$(AAPT_BUILD_FLAGS) \
		-F "$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		-M "$(ROOT_DIR)AndroidManifest.xml" \
		-S "$(ROOT_DIR)res" \
		-I "$(ADK_PLATFORMS_DIR)android.jar"
# Convert to DEX.
	@DIR /A-D /S /B $(OBJ_DIR)*.class > obj.tmp
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)d8.bat" \
		--lib "$(ADK_PLATFORMS_DIR)android.jar" \
		--min-api $(ANDROID_TARGET_VERSION) \
		--$(BUILD_NAME) \
		--output "$(ROOT_DIR).bin" \
		"@obj.tmp" \
		$(LIB_DIR)*.jar
	@DEL obj.tmp
# For some reason, aapt add only works for classes.dex in the current directory.
	@MOVE "$(BIN_DIR)classes.dex" . >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)aapt.exe" add \
		"$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		classes.dex
	@DEL classes.dex
# Add shared dependencies.
	@MKDIR lib lib\arm64-v8a lib\x86_64
	@COPY "$(ROOT_DIR)lib\arm64-v8a" lib\arm64-v8a >NUL
	@COPY "$(ROOT_DIR)lib\x86_64" lib\x86_64 >NUL
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)aapt.exe" add \
		"$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
		lib/arm64-v8a/libonyx_pen_touch_reader.so \
		lib/arm64-v8a/libc++_shared.so \
		lib/x86_64/libc++_shared.so
	@RD /Q /S lib
# Align to 4-byte boundaries.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)zipalign.exe" -f 4 \
	"$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.unaligned.apk" \
	"$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.unaligned.apk"
# Sign package.
	@CALL SET "JAVA_HOME=$(JDK_DIR)" && "$(ADK_BUILD_TOOLS_DIR)apksigner.bat" sign \
		--ks "$(ROOT_DIR)priv\$(REPO_NAME).$(BUILD_NAME).keystore" \
		--ks-pass pass:android \
		--ks-key-alias "$(REPO_NAME)_key" \
		--key-pass pass:android \
		--out "$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).apk" \
		"$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.apk"
	@DEL "$(BIN_DIR)$(REPO_NAME).$(BUILD_NAME).unsigned.apk"

# Precompiled headers.
$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pch: pch.cpp
	@ECHO Building $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pch...
	@IF NOT EXIST $(PROJ_OBJ_DIR) MD $(PROJ_OBJ_DIR)
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME) MD $(PROJ_OBJ_DIR)$(BUILD_NAME)
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\
		MD $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)
	@$(COMPILER) $(COMPILER_FLAGS) /Yc\
		/Fd$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pdb\
		/Fo$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.obj pch.cpp
pch.cpp: $(INCL_PCH)

# Build target.
$(PROJ_BIN): $(PROJ_SRC)
!IF $(PCH) == 1
	@$(MAKE) /C $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pch\
		PROJ=$(PROJ) BIN=$(BIN) SRC="$(SRC)" BUILD=$(BUILD) ARCH=$(ARCH) PCH=$(PCH)
!ENDIF
	@ECHO ---------------- [Build] $(BIN_NAME) ----------------
	@IF NOT EXIST $(PROJ_BIN_DIR) MD $(PROJ_BIN_DIR)
	@IF NOT EXIST $(PROJ_OBJ_DIR) MD $(PROJ_OBJ_DIR)
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME) MD $(PROJ_OBJ_DIR)$(BUILD_NAME)
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\
		MD $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)
	@ECHO Building $(PROJ_BIN)...
!IF $(PCH) == 0
	@$(COMPILER) $(COMPILER_FLAGS)\
		/Fd$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).pdb\
		/Fo$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\ \
		$?
!ELSE
# PDB and IDB should be derived from the PCH versions.
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).pdb\
		CP $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.pdb\
			$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).pdb
	@IF NOT EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).idb\
		IF EXIST $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.idb\
			CP $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.idb\
				$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).idb
	@$(COMPILER) $(COMPILER_FLAGS)\
		/Fd$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\$(BIN_NAME).pdb\
		/Fo$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\ \
		/Yu"rain.hpp" $?
!ENDIF
	@ECHO Linking $(PROJ_BIN)...
# It's unclear why some of the wildcards need to be escaped, but it works.
	@$(LINKER) $(LINKER_FLAGS) $(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\pch.obj\
		$(patsubsti %.cpp,%.obj,\
		$(patsubsti $(PROJ_SRC_DIR)\%,$(PROJ_OBJ_DIR)$(BUILD_NAME)\x$(ARCH)\\%,$**))

# Mark source files as out of date if any headers have changed.
# An empty target here will correctly set the modified date.
$(PROJ_SRC): $(INCL) pch.cpp
	@EXIT

# ------------------------------------------------------------------------------
!else
else
# ------------------------------------------------------------------------------

# GNU make (gmake)-specific commands.

# Ensure files with the same name as fake targets do not trigger.
.PHONY: __default _default
_default: 
	@echo `gmake` on Linux not supported. Use `nmake` on Windows instead.

# ------------------------------------------------------------------------------
endif    # gmake: close condition; nmake: not seen
!endif : # gmake: unused target; nmake close conditional
# ------------------------------------------------------------------------------
