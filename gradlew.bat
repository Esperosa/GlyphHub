@echo off
setlocal enabledelayedexpansion

set GRADLE_VERSION=8.11.1
set DIST_DIR=%~dp0.gradle\bootstrap\gradle-%GRADLE_VERSION%
set DIST_ZIP=%~dp0.gradle\bootstrap\gradle-%GRADLE_VERSION%-bin.zip
set GRADLE_EXE=%DIST_DIR%\bin\gradle.bat
set PS_CMD=powershell
where powershell >nul 2>nul
if errorlevel 1 set PS_CMD=pwsh

if not exist "%GRADLE_EXE%" (
  if not exist "%~dp0.gradle\bootstrap" mkdir "%~dp0.gradle\bootstrap"
  echo Downloading Gradle %GRADLE_VERSION%...
  %PS_CMD% -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST_ZIP%'"
  %PS_CMD% -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%DIST_ZIP%' '%~dp0.gradle\bootstrap'"
)

call "%GRADLE_EXE%" %*
exit /b %ERRORLEVEL%
