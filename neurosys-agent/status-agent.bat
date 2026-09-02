@echo off
setlocal EnableDelayedExpansion
title NeuroSys Agent - Status Check

cd /d "%~dp0"

echo ===================================================
echo  NeuroSys Agent Status
echo ===================================================
echo.

set "AGENT_PID="
for /f "tokens=*" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId"') do (
    if not "%%P"=="" set "AGENT_PID=%%P"
)

if defined AGENT_PID (
    echo Process Status : RUNNING ^(PID %AGENT_PID%^)
    
    if exist "%~dp0cache\agent-status.json" (
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$json = Get-Content '%~dp0cache\agent-status.json' -Raw | ConvertFrom-Json; Write-Host ('Server Status  : ' + (if ($json.connected) { 'CONNECTED' } else { 'RECONNECTING' })); Write-Host ('Server URL     : ' + $json.serverUrl); Write-Host ('Lab Name       : ' + $json.labName); Write-Host ('Last Heartbeat : ' + (if ($json.lastHeartbeat) { $json.lastHeartbeat } else { 'Not connected' }))"
    ) else (
        echo Server Status  : UNKNOWN ^(initializing...^)
    )
    echo.
    echo Status: RUNNING
) else (
    echo Process Status : NOT RUNNING
    echo.
    echo Status: STOPPED
)

echo.
ping -n 4 127.0.0.1 >nul
