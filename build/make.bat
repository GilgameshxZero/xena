@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION

@REM Careful to not exceed the 8192 string limit.
SET "DEPENDENCIES="..\rain\include\*" "..\include\*""
FOR /F "delims=" %%I IN ('DIR /B /S /AD ..\rain\include ..\include') DO (
	SET "DEPENDENCIES="%%I\*" !DEPENDENCIES!"
)

nmake /C %*
ENDLOCAL
