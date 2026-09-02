@echo off
title NeuroSys Cloud Monitoring Agent Daemon
cd /d "%~dp0"

echo =========================================================
echo   NeuroSys Cloud Monitoring Agent Daemon
echo =========================================================

set "JAVA_EXE=java"
if exist "%~dp0jre\bin\java.exe" (
    set "JAVA_EXE=%~dp0jre\bin\java.exe"
)

:agent_loop
set "ACTIVE_JAR="
if exist "%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar"
if not defined ACTIVE_JAR if exist "%~dp0neurosys-agent-1.0.0.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0.jar"
if not defined ACTIVE_JAR if exist "%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar"
if not defined ACTIVE_JAR if exist "%~dp0target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar" set "ACTIVE_JAR=%~dp0target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar"

if defined ACTIVE_JAR (
    echo [INFO] Starting NeuroSys Monitoring Agent Daemon using %ACTIVE_JAR%...
    "%JAVA_EXE%" -jar "%ACTIVE_JAR%"
) else (
    echo.
    echo [ERROR] Could not find executable Agent JAR file!
    echo Please make sure the Agent package has been extracted properly.
    echo.
    ping -n 10 127.0.0.1 >nul
    goto agent_loop
)

echo.
echo [WARN] Agent connection interrupted or system restarted. Auto-reconnecting in 5 seconds...
ping -n 6 127.0.0.1 >nul
goto agent_loop
