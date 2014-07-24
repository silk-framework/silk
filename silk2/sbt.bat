@REM SBT launcher script
@REM 
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir

@echo off
set SBT_HOME=%~dp0

set SBT_OPTS=-Xmx512m -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

:run

"%_JAVACMD%" %SBT_OPTS% -cp "%SBT_HOME%sbt-launch.jar" xsbt.boot.Boot %*
if ERRORLEVEL 1 goto error
goto end

:error
@endlocal
exit /B 1


:end
@endlocal
exit /B 0
