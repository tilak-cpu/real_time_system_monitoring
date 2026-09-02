@echo off
setlocal EnableDelayedExpansion
title NeuroSys Agent - Start

:: 1. Check Administrator Privileges & Elevate if Needed
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process '%~f0' -Verb RunAs" >nul 2>&1
    if %errorlevel% equ 0 exit /b
)

cd /d "%~dp0"

echo ===================================================
echo  NeuroSys Agent Control - Start
echo ===================================================
echo.

:: 2. Resolve Agent JAR File
set "ACTIVE_JAR="
if exist "%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar"
if not defined ACTIVE_JAR if exist "%~dp0neurosys-agent-1.0.0.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0.jar"
if not defined ACTIVE_JAR if exist "%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar" set "ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar"
if not defined ACTIVE_JAR if exist "%~dp0target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar" set "ACTIVE_JAR=%~dp0target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar"

if not defined ACTIVE_JAR (
    echo [ERROR] NeuroSys Agent installation files are missing!
    echo Please run setup-agent.bat first to install the Agent.
    echo.
    ping -n 4 127.0.0.1 >nul
    exit /b 1
)

:: 3. Check if already running
for /f "tokens=*" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId"') do (
    if not "%%P"=="" (
        echo NeuroSys Agent
        echo Status: RUNNING
        echo.
        ping -n 3 127.0.0.1 >nul
        exit /b 0
    )
)

:: 4. Start Scheduled Task or Background Process
echo Starting NeuroSys Agent...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Start-ScheduledTask -TaskName 'NeuroSysAgent' } else { Start-Process 'javaw.exe' -ArgumentList '-jar', '`"%ACTIVE_JAR%`"' -WorkingDirectory '%~dp0' }" >nul 2>&1

:: Wait 3 seconds for JVM startup
ping -n 4 127.0.0.1 >nul

:: 5. Verify status
set "IS_RUNNING=0"
for /f "tokens=*" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId"') do (
    if not "%%P"=="" set "IS_RUNNING=1"
)

if "%IS_RUNNING%"=="1" (
    echo.
    echo NeuroSys Agent
    echo Status: RUNNING
    echo.
) else (
    echo.
    echo [FAILED] Could not verify Agent process startup.
    echo Please run setup-agent.bat first.
    echo.
    ping -n 4 127.0.0.1 >nul
    exit /b 1
)

ping -n 3 127.0.0.1 >nul
