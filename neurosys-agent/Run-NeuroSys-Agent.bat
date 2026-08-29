@echo off
title NeuroSys Cloud Monitoring Agent Daemon
cd /d "%~dp0"

echo =========================================================
echo   NeuroSys Cloud Monitoring Agent Daemon
echo =========================================================

set "JAR_FILE=%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar"
set "TARGET_JAR=%~dp0target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar"

set "JAVA_EXE=java"
if exist "%~dp0jre\bin\java.exe" (
    set "JAVA_EXE=%~dp0jre\bin\java.exe"
)

:agent_loop
if exist "%JAR_FILE%" (
    echo [INFO] Starting NeuroSys Monitoring Agent Daemon...
    "%JAVA_EXE%" -jar "%JAR_FILE%"
) else if exist "%TARGET_JAR%" (
    echo [INFO] Starting NeuroSys Monitoring Agent Daemon...
    "%JAVA_EXE%" -jar "%TARGET_JAR%"
) else (
    echo.
    echo [ERROR] Could not find 'neurosys-agent-1.0.0-SNAPSHOT-exec.jar'!
    echo Please make sure 'neurosys-agent-1.0.0-SNAPSHOT-exec.jar' is copied
    echo in the exact same folder as 'Run-NeuroSys-Agent.bat'.
    echo.
    timeout /t 10 /nobreak >nul
    goto agent_loop
)

echo.
echo [WARN] Agent connection interrupted or system restarted. Auto-reconnecting in 5 seconds...
timeout /t 5 /nobreak >nul
goto agent_loop
