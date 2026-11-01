@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found. Install Gradle and make sure it is on PATH.
  exit /b 1
)
gradle --no-daemon clean build
