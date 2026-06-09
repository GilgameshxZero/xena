@REM Proxy file that calls `nmake` with evaluated wildcards.
@ECHO OFF

@REM Inject `nmake` with `vcvars` if not yet available.
WHERE nmake >NUL 2>NUL
IF %ERRORLEVEL% NEQ 0 (
	CALL "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
)

@REM Proxy file that calls nmake with INCL set to all header directories.
@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

@REM Careful to not exceed the 8192 string limit.
SET "INCL_PCH=..\rain\include\*"
FOR /F "delims=" %%I IN ('DIR /B /S /AD ..\include ..\rain\include') DO (
	SET "INCL_PCH=%%I\* !INCL_PCH!"
)
SET "INCL=%INCL_PCH% ..\include\*"
FOR /F "delims=" %%I IN ('DIR /B /S /AD ..\include') DO (
	SET "INCL=%%I\* !INCL!"
)

@REM Suppresses CMD terminate prompt and error code.
nmake /C %* || (
	SET "LEVEL=!ERRORLEVEL!"
	CALL;
	EXIT /B !LEVEL!
)
ENDLOCAL
