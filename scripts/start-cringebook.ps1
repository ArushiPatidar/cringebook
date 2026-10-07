# scripts/start-cringebook.ps1 -- build (optional) and (re)start cringebook on
# loopback :9899, which is where the god frontproxy sends
# cringebook.gogillu.live.
#
# The edge configuration (host route + cert dir) lives in
# god\scripts\start-frontproxy.ps1 -- that file is the SINGLE SOURCE OF TRUTH.
# Never copy it here; a duplicated edge config drifts and takes the domain
# down.
#
# Database credentials are NOT set here. They come from
# src/main/resources/application.properties, which Spring Boot reads itself.
# Only the JDBC URL is overridden, so the schema is auto-created on a fresh
# machine.

[CmdletBinding()]
param(
    [string]$RepoRoot = 'C:\Users\arushi\cringebook',
    [int]$Port = 9899,
    [string]$JavaHome = 'C:\Users\arushi\jdk21',
    [switch]$SkipBuild,
    [int]$HealthTimeoutSec = 90
)

$ErrorActionPreference = 'Stop'

$Jar    = Join-Path $RepoRoot 'target\app-0.0.1-SNAPSHOT.jar'
$LogDir = Join-Path $RepoRoot 'logs'
$PidFile = Join-Path $RepoRoot 'logs\cringebook.pid'

function Write-CbLog($msg) {
    Write-Host "[$((Get-Date).ToString('HH:mm:ss'))][cringebook] $msg"
}

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }

# -- stop whatever currently owns the port (by PID, never by name) --
$existing = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
            Select-Object -First 1
if ($existing) {
    Write-CbLog "stopping existing listener on :$Port (pid=$($existing.OwningProcess))"
    Stop-Process -Id ([int]$existing.OwningProcess) -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

if (-not $SkipBuild) {
    Write-CbLog 'building (mvnw -DskipTests package)'
    $env:JAVA_HOME = $JavaHome
    & $env:ComSpec /c "cd /d $RepoRoot && .\mvnw.cmd -B -DskipTests package"
    if ($LASTEXITCODE -ne 0) { throw "maven build failed ($LASTEXITCODE)" }
}

if (-not (Test-Path $Jar)) { throw "jar not found at $Jar -- run without -SkipBuild" }

$stamp  = (Get-Date).ToString('yyyyMMddHHmmss')
$stdout = Join-Path $LogDir "cringebook-$stamp.stdout.log"
$stderr = Join-Path $LogDir "cringebook-$stamp.stderr.log"

# Loopback-only: the public edge is the frontproxy, never this port directly.
$jdbcUrl = 'jdbc:mysql://localhost:3306/data?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'

$javaArgs = @(
    '-jar', $Jar,
    "--server.port=$Port",
    '--server.address=127.0.0.1',
    "--cringebook.frontend-dir=$(Join-Path $RepoRoot 'frontend')",
    "--cringebook.upload-dir=$(Join-Path $RepoRoot 'uploads')",
    "--spring.datasource.url=$jdbcUrl"
)

Write-CbLog "starting on 127.0.0.1:$Port (logs -> $stderr)"
$p = Start-Process -FilePath (Join-Path $JavaHome 'bin\java.exe') `
        -ArgumentList $javaArgs `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError  $stderr `
        -WindowStyle Hidden -PassThru
$p.Id | Out-File $PidFile -Encoding ASCII -NoNewline
Write-CbLog "pid=$($p.Id)"

for ($i = 0; $i -lt $HealthTimeoutSec; $i++) {
    Start-Sleep -Seconds 1
    if ($p.HasExited) {
        $tail = if (Test-Path $stdout) { Get-Content $stdout -Tail 40 -Raw } else { '' }
        throw "cringebook exited during startup. log tail:`n$tail"
    }
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/healthz" -TimeoutSec 3 -UseBasicParsing
        if ($r.StatusCode -eq 200) {
            Write-CbLog "healthy on :$Port after ${i}s"
            return $p.Id
        }
    } catch { }
}

$tail = if (Test-Path $stdout) { Get-Content $stdout -Tail 40 -Raw } else { '' }
throw "cringebook did not become healthy within ${HealthTimeoutSec}s. log tail:`n$tail"
