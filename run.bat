@echo off
title JavaBank Launcher
echo Checking Java Environment...
java -version
if %errorlevel% neq 0 (
    echo Java is not installed or not in PATH! Please check prerequisites.
    pause
    exit /b
)
echo Launching JavaBank Application...
cd /d "%~dp0"
call mvnw.cmd javafx:run
if %errorlevel% neq 0 (
    echo Failed to launch JavaBank!
    pause
)
