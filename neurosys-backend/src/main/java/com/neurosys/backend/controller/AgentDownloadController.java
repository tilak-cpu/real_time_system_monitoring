package com.neurosys.backend.controller;

import com.neurosys.backend.entity.Lab;
import com.neurosys.backend.entity.LabEnrollmentCode;
import com.neurosys.backend.exception.ResourceNotFoundException;
import com.neurosys.backend.repository.LabEnrollmentCodeRepository;
import com.neurosys.backend.repository.LabRepository;
import com.neurosys.backend.service.LabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Download & Package Generator", description = "REST APIs for generating and downloading pre-configured agent setup packages for specific computer labs")
public class AgentDownloadController {

    private final LabRepository labRepository;
    private final LabEnrollmentCodeRepository enrollmentCodeRepository;
    private final LabService labService;

    @GetMapping("/download")
    @Operation(summary = "Download Lab-Preconfigured NeuroSys Agent Package", description = "Generates a .zip package containing agent binary, pre-configured agent.properties, and enrollment code for the target lab")
    public ResponseEntity<byte[]> downloadAgentPackage(
            @RequestParam(required = false) String labId,
            @RequestParam(required = false) String enrollmentCode) {
        
        Lab lab;
        if (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId)) {
            lab = labRepository.findById(labId)
                    .orElseGet(() -> labRepository.findByCodeIgnoreCase(labId)
                    .orElseGet(() -> labRepository.findAll().stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Lab", "id", labId))));
        } else {
            lab = labRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Lab", "default", "none"));
        }

        String activeCode = enrollmentCode;
        if (activeCode == null || activeCode.isEmpty()) {
            Optional<LabEnrollmentCode> existing = enrollmentCodeRepository.findByLabIdOrderByCreatedAtDesc(lab.getId())
                    .stream().filter(c -> !c.getRevoked() && c.getExpiresAt().isAfter(Instant.now())).findFirst();
            if (existing.isPresent()) {
                activeCode = existing.get().getCode();
            } else {
                var newCode = labService.generateEnrollmentCode(lab.getId(), null, "system");
                activeCode = newCode.getCode();
            }
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 1. Write agent.properties
            String propsContent = String.format(
                    "# NeuroSys Agent Configuration Package\n" +
                    "# Generated for: %s (%s)\n" +
                    "server.url=https://realtimesystemmonitoring-production.up.railway.app/api/v1\n" +
                    "agent.enrollment.code=%s\n" +
                    "agent.lab.name=%s\n" +
                    "agent.collection.interval.seconds=1\n",
                    lab.getName(), lab.getCode(), activeCode, lab.getName()
            );
            writeZipEntry(zos, "agent.properties", propsContent.getBytes());

            // 2. Write start-agent.bat
            String startBatContent =
                    "@echo off\r\n" +
                    "setlocal EnableDelayedExpansion\r\n" +
                    "title NeuroSys Agent - Start\r\n" +
                    "net session >nul 2>&1\r\n" +
                    "if %errorlevel% neq 0 (\r\n" +
                    "    powershell -NoProfile -ExecutionPolicy Bypass -Command \"Start-Process '%~f0' -Verb RunAs\" >nul 2>&1\r\n" +
                    "    if %errorlevel% equ 0 exit /b\r\n" +
                    ")\r\n" +
                    "cd /d \"%~dp0\"\r\n" +
                    "echo ===================================================\r\n" +
                    "echo  NeuroSys Agent Control - Start\r\n" +
                    "echo ===================================================\r\n" +
                    "echo.\r\n" +
                    "set \"ACTIVE_JAR=\"\r\n" +
                    "if exist \"%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0neurosys-agent-1.0.0.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0target\\neurosys-agent-1.0.0-SNAPSHOT-exec.jar\" set \"ACTIVE_JAR=%~dp0target\\neurosys-agent-1.0.0-SNAPSHOT-exec.jar\"\r\n" +
                    "if not defined ACTIVE_JAR (\r\n" +
                    "    echo [ERROR] NeuroSys Agent installation files are missing!\r\n" +
                    "    echo Please run setup-agent.bat first to install the Agent.\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 4 127.0.0.1 >nul\r\n" +
                    "    exit /b 1\r\n" +
                    ")\r\n" +
                    "for /f \"tokens=*\" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command \"Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId\"') do (\r\n" +
                    "    if not \"%%P\"==\"\" (\r\n" +
                    "        echo NeuroSys Agent\r\n" +
                    "        echo Status: RUNNING\r\n" +
                    "        echo.\r\n" +
                    "        ping -n 3 127.0.0.1 >nul\r\n" +
                    "        exit /b 0\r\n" +
                    "    )\r\n" +
                    ")\r\n" +
                    "echo Starting NeuroSys Agent...\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Start-ScheduledTask -TaskName 'NeuroSysAgent' } else { Start-Process 'javaw.exe' -ArgumentList '-jar', '`\"%ACTIVE_JAR%`\"' -WorkingDirectory '%~dp0' }\" >nul 2>&1\r\n" +
                    "ping -n 5 127.0.0.1 >nul\r\n" +
                    "set \"IS_RUNNING=0\"\r\n" +
                    "for /f \"tokens=*\" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command \"Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId\"') do (\r\n" +
                    "    if not \"%%P\"==\"\" set \"IS_RUNNING=1\"\r\n" +
                    ")\r\n" +
                    "if \"%IS_RUNNING%\"==\"1\" (\r\n" +
                    "    echo.\r\n" +
                    "    echo NeuroSys Agent\r\n" +
                    "    echo Status: RUNNING\r\n" +
                    "    echo.\r\n" +
                    ") else (\r\n" +
                    "    echo.\r\n" +
                    "    echo [FAILED] Could not verify Agent process startup.\r\n" +
                    "    echo Please run setup-agent.bat first.\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 4 127.0.0.1 >nul\r\n" +
                    "    exit /b 1\r\n" +
                    ")\r\n" +
                    "ping -n 3 127.0.0.1 >nul\r\n";
            writeZipEntry(zos, "start-agent.bat", startBatContent.getBytes());

            // 3. Write stop-agent.bat
            String stopBatContent =
                    "@echo off\r\n" +
                    "setlocal EnableDelayedExpansion\r\n" +
                    "title NeuroSys Agent - Stop\r\n" +
                    "net session >nul 2>&1\r\n" +
                    "if %errorlevel% neq 0 (\r\n" +
                    "    powershell -NoProfile -ExecutionPolicy Bypass -Command \"Start-Process '%~f0' -Verb RunAs\" >nul 2>&1\r\n" +
                    "    if %errorlevel% equ 0 exit /b\r\n" +
                    ")\r\n" +
                    "cd /d \"%~dp0\"\r\n" +
                    "echo ===================================================\r\n" +
                    "echo  NeuroSys Agent Control - Stop\r\n" +
                    "echo ===================================================\r\n" +
                    "echo.\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Stop-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue }\" >nul 2>&1\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$procs = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; foreach ($p in $procs) { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue }\" >nul 2>&1\r\n" +
                    "ping -n 3 127.0.0.1 >nul\r\n" +
                    "set \"STILL_RUNNING=0\"\r\n" +
                    "for /f \"tokens=*\" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -Command \"Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' } | Select-Object -ExpandProperty ProcessId\"') do (\r\n" +
                    "    if not \"%%P\"==\"\" set \"STILL_RUNNING=1\"\r\n" +
                    ")\r\n" +
                    "if \"%STILL_RUNNING%\"==\"0\" (\r\n" +
                    "    echo.\r\n" +
                    "    echo NeuroSys Agent\r\n" +
                    "    echo Status: STOPPED\r\n" +
                    "    echo.\r\n" +
                    ") else (\r\n" +
                    "    echo.\r\n" +
                    "    echo [FAILED] Agent process could not be terminated.\r\n" +
                    "    echo Status: RUNNING\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 4 127.0.0.1 >nul\r\n" +
                    "    exit /b 1\r\n" +
                    ")\r\n" +
                    "ping -n 3 127.0.0.1 >nul\r\n";
            writeZipEntry(zos, "stop-agent.bat", stopBatContent.getBytes());

            // 4. Write uninstall-agent.bat
            String uninstallBatContent =
                    "@echo off\r\n" +
                    "setlocal EnableDelayedExpansion\r\n" +
                    "title NeuroSys Agent Uninstaller\r\n" +
                    "net session >nul 2>&1\r\n" +
                    "if %errorlevel% neq 0 (\r\n" +
                    "    powershell -NoProfile -ExecutionPolicy Bypass -Command \"Start-Process '%~f0' -Verb RunAs\" >nul 2>&1\r\n" +
                    "    if %errorlevel% equ 0 exit /b\r\n" +
                    ")\r\n" +
                    "set \"AGENT_DIR=%~dp0\"\r\n" +
                    "if \"%AGENT_DIR:~-1%\"==\"\\\" set \"AGENT_DIR=%AGENT_DIR:~0,-1%\"\r\n" +
                    "echo.\r\n" +
                    "echo ===================================================\r\n" +
                    "echo  NeuroSys Agent Uninstaller\r\n" +
                    "echo ===================================================\r\n" +
                    "echo.\r\n" +
                    "set \"AGENT_STOPPED=FAILED\"\r\n" +
                    "set \"TASK_REMOVED=FAILED\"\r\n" +
                    "set \"PROCESS_TERMINATED=FAILED\"\r\n" +
                    "set \"RUNTIME_REMOVED=FAILED\"\r\n" +
                    "set \"INSTALLATION_REMOVED=FAILED\"\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"if (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue) { Stop-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue; Unregister-ScheduledTask -TaskName 'NeuroSysAgent' -Confirm:\\$false -ErrorAction SilentlyContinue }\" >nul 2>&1\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"if (-not (Get-ScheduledTask -TaskName 'NeuroSysAgent' -ErrorAction SilentlyContinue)) { exit 0 } else { exit 1 }\" >nul 2>&1\r\n" +
                    "if %errorlevel% equ 0 (\r\n" +
                    "    set \"TASK_REMOVED=OK\"\r\n" +
                    "    set \"AGENT_STOPPED=OK\"\r\n" +
                    ") else (\r\n" +
                    "    set \"TASK_REMOVED=FAILED\"\r\n" +
                    ")\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$u = [Environment]::GetFolderPath('Startup'); $uL = Join-Path $u 'NeuroSysAgent.lnk'; if (Test-Path $uL) { Remove-Item $uL -Force -ErrorAction SilentlyContinue }; $c = [Environment]::GetFolderPath('CommonStartup'); $cL = Join-Path $c 'NeuroSysAgent.lnk'; if (Test-Path $cL) { Remove-Item $cL -Force -ErrorAction SilentlyContinue }\" >nul 2>&1\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$procs = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; foreach ($p in $procs) { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue }\" >nul 2>&1\r\n" +
                    "ping -n 3 127.0.0.1 >nul\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$p = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -like '*neurosys-agent*' }; if (-not $p) { exit 0 } else { exit 1 }\" >nul 2>&1\r\n" +
                    "if %errorlevel% equ 0 (\r\n" +
                    "    set \"PROCESS_TERMINATED=OK\"\r\n" +
                    ") else (\r\n" +
                    "    set \"PROCESS_TERMINATED=FAILED\"\r\n" +
                    ")\r\n" +
                    "if exist \"%AGENT_DIR%\\cache\" rmdir /s /q \"%AGENT_DIR%\\cache\" >nul 2>&1\r\n" +
                    "if exist \"%AGENT_DIR%\\logs\" rmdir /s /q \"%AGENT_DIR%\\logs\" >nul 2>&1\r\n" +
                    "set \"RUNTIME_REMOVED=OK\"\r\n" +
                    "set \"INSTALLATION_REMOVED=OK\"\r\n" +
                    "echo [%AGENT_STOPPED%] Agent stopped\r\n" +
                    "echo [%TASK_REMOVED%] Scheduled task removed\r\n" +
                    "echo [%PROCESS_TERMINATED%] Agent process terminated\r\n" +
                    "echo [%RUNTIME_REMOVED%] Runtime files removed\r\n" +
                    "echo [%INSTALLATION_REMOVED%] Installation removed\r\n" +
                    "echo.\r\n" +
                    "if \"%PROCESS_TERMINATED%\"==\"OK\" if \"%TASK_REMOVED%\"==\"OK\" (\r\n" +
                    "    echo NeuroSys Agent has been successfully uninstalled.\r\n" +
                    "    echo.\r\n" +
                    "    echo You can now delete the ZIP package.\r\n" +
                    "    echo.\r\n" +
                    "    powershell -NoProfile -ExecutionPolicy Bypass -Command \"Start-Process powershell -ArgumentList '-NoProfile', '-Command', 'Start-Sleep -Seconds 4; Remove-Item `\"%AGENT_DIR%`\" -Recurse -Force' -WindowStyle Hidden\" >nul 2>&1\r\n" +
                    "    ping -n 3 127.0.0.1 >nul\r\n" +
                    "    exit /b 0\r\n" +
                    ") else (\r\n" +
                    "    echo [FAILED] NeuroSys Agent was NOT completely removed.\r\n" +
                    "    echo Please try again as Administrator.\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 4 127.0.0.1 >nul\r\n" +
                    "    exit /b 1\r\n" +
                    ")\r\n";
            writeZipEntry(zos, "uninstall-agent.bat", uninstallBatContent.getBytes());

            // 5. Write setup-agent.bat
            String setupBatContent =
                    "@echo off\r\n" +
                    "setlocal\r\n" +
                    "cd /d \"%~dp0\"\r\n" +
                    "net session >nul 2>&1\r\n" +
                    "if %errorlevel% neq 0 (\r\n" +
                    "    echo Requesting Administrative privileges to install NeuroSys Agent...\r\n" +
                    "    powershell -NoProfile -ExecutionPolicy Bypass -Command \"Start-Process '%~f0' -Verb RunAs\" >nul 2>&1\r\n" +
                    "    if %errorlevel% equ 0 exit /b\r\n" +
                    ")\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -File \"%~dp0install-service.ps1\"\r\n" +
                    "if %errorlevel% neq 0 (\r\n" +
                    "    echo.\r\n" +
                    "    echo [FATAL ERROR] Agent setup failed with error code %errorlevel%.\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 5 127.0.0.1 >nul\r\n" +
                    "    exit /b %errorlevel%\r\n" +
                    ")\r\n" +
                    "ping -n 3 127.0.0.1 >nul\r\n";
            writeZipEntry(zos, "setup-agent.bat", setupBatContent.getBytes());

            // 6. Write Run-NeuroSys-Agent.bat
            String runBatContent =
                    "@echo off\r\n" +
                    "title NeuroSys Cloud Monitoring Agent Daemon\r\n" +
                    "cd /d \"%~dp0\"\r\n" +
                    "echo =========================================================\r\n" +
                    "echo   NeuroSys Cloud Monitoring Agent Daemon\r\n" +
                    "echo =========================================================\r\n" +
                    "set \"JAVA_EXE=java\"\r\n" +
                    "if exist \"%~dp0jre\\bin\\java.exe\" (\r\n" +
                    "    set \"JAVA_EXE=%~dp0jre\\bin\\java.exe\"\r\n" +
                    ")\r\n" +
                    ":agent_loop\r\n" +
                    "set \"ACTIVE_JAR=\"\r\n" +
                    "if exist \"%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT-exec.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0neurosys-agent-1.0.0.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar\" set \"ACTIVE_JAR=%~dp0neurosys-agent-1.0.0-SNAPSHOT.jar\"\r\n" +
                    "if not defined ACTIVE_JAR if exist \"%~dp0target\\neurosys-agent-1.0.0-SNAPSHOT-exec.jar\" set \"ACTIVE_JAR=%~dp0target\\neurosys-agent-1.0.0-SNAPSHOT-exec.jar\"\r\n" +
                    "if defined ACTIVE_JAR (\r\n" +
                    "    echo [INFO] Starting NeuroSys Monitoring Agent Daemon using %ACTIVE_JAR%...\r\n" +
                    "    \"%JAVA_EXE%\" -jar \"%ACTIVE_JAR%\"\r\n" +
                    ") else (\r\n" +
                    "    echo.\r\n" +
                    "    echo [ERROR] Could not find executable Agent JAR file!\r\n" +
                    "    echo Please make sure the Agent package has been extracted properly.\r\n" +
                    "    echo.\r\n" +
                    "    ping -n 10 127.0.0.1 >nul\r\n" +
                    "    goto agent_loop\r\n" +
                    ")\r\n" +
                    "echo.\r\n" +
                    "echo [WARN] Agent connection interrupted or system restarted. Auto-reconnecting in 5 seconds...\r\n" +
                    "ping -n 6 127.0.0.1 >nul\r\n" +
                    "goto agent_loop\r\n";
            writeZipEntry(zos, "Run-NeuroSys-Agent.bat", runBatContent.getBytes());

            // 7. Write install-service.ps1
            String ps1Content =
                    "$ErrorActionPreference = 'Stop'\r\n" +
                    "$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path\r\n" +
                    "Set-Location $ScriptDir\r\n" +
                    "Write-Host '===================================================' -ForegroundColor Cyan\r\n" +
                    "Write-Host ' NeuroSys Telemetry Agent - Automatic Service Installer' -ForegroundColor Cyan\r\n" +
                    "Write-Host '===================================================' -ForegroundColor Cyan\r\n" +
                    "$TaskName = 'NeuroSysAgent'\r\n" +
                    "$BatLauncher = Join-Path $ScriptDir 'Run-NeuroSys-Agent.bat'\r\n" +
                    "try {\r\n" +
                    "    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null\r\n" +
                    "    $Action = New-ScheduledTaskAction -Execute 'cmd.exe' -ArgumentList \"/c `\"$BatLauncher`\"\" -WorkingDirectory $ScriptDir\r\n" +
                    "    $TriggerLogon = New-ScheduledTaskTrigger -AtLogon\r\n" +
                    "    $Settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable\r\n" +
                    "    Register-ScheduledTask -TaskName $TaskName -Action $Action -Trigger $TriggerLogon -Settings $Settings -User $env:USERNAME -Force | Out-Null\r\n" +
                    "    Write-Host ' [OK] Task registered successfully.' -ForegroundColor Green\r\n" +
                    "    Start-ScheduledTask -TaskName $TaskName | Out-Null\r\n" +
                    "} catch {\r\n" +
                    "    Write-Host ' [NOTICE] Running background agent fallback.' -ForegroundColor Yellow\r\n" +
                    "    Start-Process 'cmd.exe' -ArgumentList \"/c `\"$BatLauncher`\"\" -WorkingDirectory $ScriptDir -WindowStyle Minimized\r\n" +
                    "}\r\n";
            writeZipEntry(zos, "install-service.ps1", ps1Content.getBytes());

            // 8. Write README-INSTALLATION.txt
            String readmeContent = String.format(
                    "=========================================================\n" +
                    "NEUROSYS AGENT ONBOARDING PACKAGE\n" +
                    "Target Laboratory: %s (%s)\n" +
                    "Enrollment Code:   %s\n" +
                    "Generated At:      %s\n" +
                    "=========================================================\n\n" +
                    "INSTRUCTIONS FOR SYSTEM ADMINISTRATORS:\n\n" +
                    "1. Extract all files in this ZIP archive to a folder on the target Windows workstation.\n" +
                    "2. Run 'start-agent.bat' to start the agent immediately.\n" +
                    "3. Run 'stop-agent.bat' to stop the agent.\n" +
                    "4. Run 'uninstall-agent.bat' for a complete clean uninstallation.\n\n" +
                    "Need assistance? Contact your NeuroSys Lab Supervisor.\n",
                    lab.getName(), lab.getCode(), activeCode, Instant.now().toString()
            );
            writeZipEntry(zos, "README-INSTALLATION.txt", readmeContent.getBytes());

            // 9. Attach compiled neurosys-agent JAR from classpath or filesystem
            byte[] jarBytes = null;
            try (InputStream is = getClass().getResourceAsStream("/static/agent-bin/neurosys-agent-1.0.0-SNAPSHOT-exec.jar")) {
                if (is != null) {
                    jarBytes = is.readAllBytes();
                }
            } catch (Exception e) {
                log.warn("Could not read agent jar from classpath resource: {}", e.getMessage());
            }

            if (jarBytes == null || jarBytes.length == 0) {
                Path agentJarPath = Paths.get("..", "neurosys-agent", "target", "neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
                if (!Files.exists(agentJarPath)) {
                    agentJarPath = Paths.get("neurosys-agent", "target", "neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
                }
                if (!Files.exists(agentJarPath)) {
                    agentJarPath = Paths.get("neurosys-agent-1.0.0-SNAPSHOT-exec.jar");
                }
                if (Files.exists(agentJarPath)) {
                    jarBytes = Files.readAllBytes(agentJarPath);
                }
            }

            if (jarBytes != null && jarBytes.length > 0) {
                writeZipEntry(zos, "neurosys-agent-1.0.0-SNAPSHOT-exec.jar", jarBytes);
                writeZipEntry(zos, "neurosys-agent-1.0.0.jar", jarBytes);
            }

            zos.finish();
            zos.close();

            byte[] zipData = baos.toByteArray();
            String filename = String.format("neurosys-agent-%s.zip", lab.getCode().replaceAll("[^A-Za-z0-9]", ""));

            log.info("[INFO] Generated Agent Package ZIP for Lab {} ({}), Size: {} bytes", lab.getName(), lab.getCode(), zipData.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipData);

        } catch (Exception e) {
            log.error("Failed to generate agent download package ZIP", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void writeZipEntry(ZipOutputStream zos, String entryName, byte[] data) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }
}
