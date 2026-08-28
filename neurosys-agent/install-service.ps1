# NeuroSys Telemetry Agent - Windows Service Installer (PowerShell)
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " NeuroSys Telemetry Agent - Installer" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host ""

# [1/5] Checking files...
Write-Host "[1/5] Checking files..." -NoNewline
$JarPath = Join-Path $ScriptDir "neurosys-agent-1.0.0-SNAPSHOT-exec.jar"
$TargetJar = Join-Path $ScriptDir "target\neurosys-agent-1.0.0-SNAPSHOT-exec.jar"

if (-not (Test-Path $JarPath)) {
    if (Test-Path $TargetJar) {
        Copy-Item -Path $TargetJar -Destination $JarPath -Force
    } else {
        Write-Host " [FAILED]" -ForegroundColor Red
        Write-Host ""
        Write-Host "[ERROR] Agent executable JAR missing: $JarPath" -ForegroundColor Red
        exit 1
    }
}
Write-Host " [OK] Agent JAR found." -ForegroundColor Green

# Ensure agent.properties exists with Production Railway URL
$PropsPath = Join-Path $ScriptDir "agent.properties"
if (-not (Test-Path $PropsPath)) {
    $DefaultProps = @"
# NeuroSys Monitoring Agent Production Configuration
server.url=https://realtimesystemmonitoring-production.up.railway.app/api/v1
agent.lab.name=Computer Lab
agent.collection.interval.seconds=1
agent.cache.dir=./cache
"@
    Set-Content -Path $PropsPath -Value $DefaultProps -Encoding UTF8
}

# [2/5] Checking Java...
Write-Host "[2/5] Checking Java..." -NoNewline
$BundledJava = Join-Path $ScriptDir "jre\bin\java.exe"
$JavaExe = $null

if (Test-Path $BundledJava) {
    $JavaExe = $BundledJava
} else {
    $SystemJava = Get-Command "java" -ErrorAction SilentlyContinue
    if ($SystemJava) {
        $JavaExe = $SystemJava.Source
    }
}

if (-not $JavaExe) {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host ""
    Write-Host "[ERROR] Java Runtime (JRE 17+ or JDK 21) was not found in PATH or ./jre folder." -ForegroundColor Red
    exit 1
}
Write-Host " [OK] Java found." -ForegroundColor Green

# [3/5] Registering NeuroSysAgent...
Write-Host "[3/5] Registering NeuroSysAgent..." -NoNewline
$TaskName = "NeuroSysAgent"

try {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
    
    $Action = New-ScheduledTaskAction -Execute $JavaExe -Argument "-jar `"$JarPath`"" -WorkingDirectory $ScriptDir
    $Trigger = New-ScheduledTaskTrigger -AtStartup
    $Settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RunOnlyIfNetworkAvailable -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1)

    $Registered = $false
    try {
        $Principal = New-ScheduledTaskPrincipal -UserId "NT AUTHORITY\SYSTEM" -LogonType ServiceAccount -RunLevel Highest
        Register-ScheduledTask -TaskName $TaskName -Action $Action -Trigger $Trigger -Settings $Settings -Principal $Principal | Out-Null
        $Registered = $true
    } catch {
        # Fallback to User account task registration
        $UserTrigger = New-ScheduledTaskTrigger -AtLogon
        Register-ScheduledTask -TaskName $TaskName -Action $Action -Trigger $UserTrigger -Settings $Settings -User $env:USERNAME | Out-Null
        $Registered = $true
    }

    if ($Registered) {
        Write-Host " [OK] Scheduled task registered." -ForegroundColor Green
    } else {
        throw "Failed to register scheduled task"
    }
} catch {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host ""
    Write-Host "[ERROR] Failed to register Scheduled Task: $_" -ForegroundColor Red
    exit 1
}

# [4/5] Starting NeuroSysAgent...
Write-Host "[4/5] Starting NeuroSysAgent..." -NoNewline
try {
    Start-ScheduledTask -TaskName $TaskName | Out-Null
    Start-Sleep -Seconds 2
    Write-Host " [OK] Agent started." -ForegroundColor Green
} catch {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host ""
    Write-Host "[ERROR] Failed to start task: $_" -ForegroundColor Red
    exit 1
}

# [5/5] Verifying status...
Write-Host "[5/5] Verifying status..." -NoNewline
$Task = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($Task -and ($Task.State -eq 'Running' -or $Task.State -eq 'Ready')) {
    Write-Host " [OK] NeuroSysAgent is $($Task.State.ToString().ToUpper())." -ForegroundColor Green
} else {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host ""
    Write-Host "[ERROR] NeuroSysAgent is not running." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Installation completed successfully." -ForegroundColor Green
Write-Host " Agent will start automatically with Windows." -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host ""
