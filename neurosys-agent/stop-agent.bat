@echo off
setlocal EnableDelayedExpansion
title NeuroSys Agent - Stop

:: 1. Check Administrator Privileges & Elevate if Needed
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process '%~f0' -Verb RunAs" >nul 2>&1
    if %errorlevel% equ 0 exit /b
)

cd /d "%~dp0"

echo ===================================================
echo  NeuroSys Agent Control - Stop
echo ===================================================
echo.

:: 2. Stop Scheduled Task if registered
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Stop-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue }" >nul 2>&1

:: 3. Safely terminate ONLY Java processes running neurosys-agent
powershell -NoProfile -ExecutionPolicy Bypass -Command "$procs = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; foreach ($p in $procs) { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1

:: Wait 2 seconds for process exit
ping -n 3 127.0.0.1 >nul

:: 4. Verify process is stopped
set "STILL_RUNNING=0"
for /f "tokens=*" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId"') do (
    if not "%%P"=="" set "STILL_RUNNING=1"
)

if "%STILL_RUNNING%"=="0" (
    echo.
    echo NeuroSys Agent
    echo Status: STOPPED
    echo.
) else (
    echo.
    echo [FAILED] Agent process could not be terminated.
    echo Status: RUNNING
    echo.
    ping -n 4 127.0.0.1 >nul
    exit /b 1
)

ping -n 3 127.0.0.1 >nul
