@REM Increments build version number at compile-time, and compiles the version numbers into all relevant files for compilation.
@ECHO OFF
@REM LF newline variable.
(SET \n=^

)
SETLOCAL ENABLEDELAYEDEXPANSION

SET ROOT_DIR=%~dp0..\
SET REPO_NAME=%1

SET /P VERSION_MAJOR=<version.major.txt
SET /P VERSION_MINOR=<version.minor.txt
SET /P VERSION_REVISION=<version.revision.txt
SET /P VERSION_BUILD=<version.build.txt
SET /A VERSION_BUILD=!VERSION_BUILD!+1
<NUL SET /P=!VERSION_BUILD!!\n!> version.build.txt

@REM .exe build.
<NUL SET /P=^
#pragma once!\n!^
!\n!^
#define !REPO_NAME!_VERSION_MAJOR !VERSION_MAJOR!!\n!^
#define !REPO_NAME!_VERSION_MINOR !VERSION_MINOR!!\n!^
#define !REPO_NAME!_VERSION_REVISION !VERSION_REVISION!!\n!^
#define !REPO_NAME!_VERSION_BUILD !VERSION_BUILD!!\n!> ^
version.hpp

@REM .apk build. ^ for newline continuation consumes the next character, so we
@REM have it consume a ^ in some instances. In other cases we want a tab.
<NUL SET /P=^
	^<?xml version="1.0" encoding="utf-8"?^>!\n!^
^^<resources^>!\n!^
	^<string name="xena_version"^>^
!VERSION_MAJOR!.!VERSION_MINOR!.!VERSION_REVISION!.!VERSION_BUILD!^
^^</string^>!\n!^
^^</resources^>!\n!> ^
!ROOT_DIR!res\values\version.xml

ECHO Version !VERSION_MAJOR!.!VERSION_MINOR!.!VERSION_REVISION!.!VERSION_BUILD!.

ENDLOCAL
