@echo off
setlocal EnableDelayedExpansion
title NeuroSys Agent Uninstaller

:: 1. Check Administrator Privileges & Elevate if Needed
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process '%~f0' -Verb RunAs" >nul 2>&1
    if %errorlevel% equ 0 exit /b
)

set "AGENT_DIR=%~dp0"
if "%AGENT_DIR:~-1%"=="\" set "AGENT_DIR=%AGENT_DIR:~0,-1%"

echo.
echo ===================================================
echo  NeuroSys Agent Uninstaller
echo ===================================================
echo.

set "AGENT_STOPPED=FAILED"
set "TASK_REMOVED=FAILED"
set "PROCESS_TERMINATED=FAILED"
set "RUNTIME_REMOVED=FAILED"
set "INSTALLATION_REMOVED=FAILED"

:: 2. Stop and Unregister Scheduled Task
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Stop-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue; Unregister-ScheduledTask -TaskName 'NeuroSysAgent' -Confirm:\$false -ErrorAction SilentlyContinue }" >nul 2>&1

:: Verify Scheduled Task removal
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (-not (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue)) { exit 0 } else { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 (
    set "TASK_REMOVED=OK"
    set "AGENT_STOPPED=OK"
) else (
    set "TASK_REMOVED=FAILED"
)

:: 3. Remove Windows Startup Shortcuts
powershell -NoProfile -ExecutionPolicy Bypass -Command "$u = [Environment]::GetFolderPath('Startup'); $uL = Join-Path $u 'NeuroSysAgent.lnk'; if (Test-Path $uL) { Remove-Item $uL -Force -ErrorAction SilentlyContinue }; $c = [Environment]::GetFolderPath('CommonStartup'); $cL = Join-Path $c 'NeuroSysAgent.lnk'; if (Test-Path $cL) { Remove-Item $cL -Force -ErrorAction SilentlyContinue }" >nul 2>&1

:: 4. Safely Terminate ONLY NeuroSys Agent Java Process
powershell -NoProfile -ExecutionPolicy Bypass -Command "$procs = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; foreach ($p in $procs) { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1
ping -n 3 127.0.0.1 >nul

:: Verify Process Termination
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; if (-not $p) { exit 0 } else { exit 1 }" >nul 2>&1
if %errorlevel% equ 0 (
    set "PROCESS_TERMINATED=OK"
) else (
    set "PROCESS_TERMINATED=FAILED"
)

:: 5. Remove Runtime Cache and Logs
if exist "%AGENT_DIR%\cache" rmdir /s /q "%AGENT_DIR%\cache" >nul 2>&1
if exist "%AGENT_DIR%\logs" rmdir /s /q "%AGENT_DIR%\logs" >nul 2>&1
set "RUNTIME_REMOVED=OK"
set "INSTALLATION_REMOVED=OK"

echo [%AGENT_STOPPED%] Agent stopped
echo [%TASK_REMOVED%] Scheduled task removed
echo [%PROCESS_TERMINATED%] Agent process terminated
echo [%RUNTIME_REMOVED%] Runtime files removed
echo [%INSTALLATION_REMOVED%] Installation removed
echo.

if "%PROCESS_TERMINATED%"=="OK" if "%TASK_REMOVED%"=="OK" (
    echo NeuroSys Agent has been successfully uninstalled.
    echo.
    echo You can now delete the ZIP package.
    echo.

    :: Spawn detached background worker outside directory to delete installation folder after script exits
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process powershell -ArgumentList '-NoProfile', '-Command', 'Start-Sleep -Seconds 4; Remove-Item `"%AGENT_DIR%`" -Recurse -Force' -WindowStyle Hidden" >nul 2>&1

    ping -n 3 127.0.0.1 >nul
    exit /b 0
) else (
    echo [FAILED] NeuroSys Agent was NOT completely removed.
    echo Please try again as Administrator.
    echo.
    ping -n 4 127.0.0.1 >nul
    exit /b 1
)
