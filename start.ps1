<#
  start.ps1 - one-shot bring-up for GIS logistics system.
  Brings up MySQL (docker), OSRM (optional), Spring Boot backend, Vite frontend.
  Usage: .\start.ps1 [-SkipOsmr] [-DbHost 127.0.0.1] [-DbPort 3306] [-DbPassword <pass>]
#>
param(
  [switch]$SkipOsmr,
  [string]$DbHost = '127.0.0.1',
  [int]$DbPort = 3306,
  [string]$DbName = 'logistics_db',
  [string]$DbUser = 'root',
  [string]$DbPassword = 'root'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
function Write-Step($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }
# ---------- 1) MySQL ----------
Write-Step 'MySQL'
if (Get-Command docker -ErrorAction SilentlyContinue) {
  $exists = docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq 'gis-mysql' }
  if (-not $exists) {
    docker run -d --name gis-mysql -p $DbPort:3306 -e MYSQL_ROOT_PASSWORD=$DbPassword -e MYSQL_DATABASE=$DbName mysql:8.0
  } else {
    docker start gis-mysql | Out-Null
  }
  Write-Host 'waiting for MySQL ...'
  $ok = $false
  for ($i = 0; $i -lt 60; $i++) {
    try {
      Invoke-WebRequest "http://$DbHost" -UseBasicParsing -TimeoutSec 1 -ErrorAction Stop | Out-Null
      $ok = $true; break
    } catch { Start-Sleep -Seconds 2 }
  }
} else {
  Write-Warning 'docker not available, assuming local MySQL is running.'
}
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    & mysql -h $DbHost -P $DbPort -u $DbUser -p$DbPassword -e "CREATE DATABASE IF NOT EXISTS $DbName;" 2>$null
    Write-Host "MySQL: ensured database $DbName"
} else {
    Write-Warning "mysql client not found: skipping CREATE DATABASE (assume $DbName already exists on $DbHost:3306)"
}
if (-not $SkipOsmr) {
    if (Get-Command osrm-routed -ErrorAction SilentlyContinue) {
        Start-Process -FilePath 'osrm-routed' -ArgumentList '--algorithm','dijkstra','--max-rounds','2' -WindowStyle Hidden
        Write-Host "OSRM started (dijkstra)"
    } else {
        Write-Warning 'osrm-routed not installed, skipping OSRM.'
    }
}
Write-Step 'Spring Boot backend'
$env:JAVA_HOME = 'E:\develop-java\jdk21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_HOST = $DbHost
$env:DB_PORT = "$DbPort"
$env:DB_NAME = $DbName
$env:DB_USER = $DbUser
$env:DB_PASSWORD = $DbPassword
$env:JWT_SECRET = '<REDACTED>'
$env:PHONE_ENCRYPTION_KEY = '<REDACTED>'
if (-not $SkipOsmr) { $env:OSRM_BASE_URL = 'http://127.0.0.1:5000' }
Set-Location (Join-Path $root 'server')
Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run','-Dspring-boot.run.jvmArguments=-Xms256m' -WindowStyle Hidden
Write-Host 'waiting for backend http://127.0.0.1:8080 ...'
$ok = $false
for ($i = 0; $i -lt 90; $i++) {
  try {
    $r = Invoke-WebRequest 'http://127.0.0.1:8080/api/v1/admin/orders/stats' -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
    if ($r.StatusCode -in 200,401,403) { $ok = $true; break }
  } catch { Start-Sleep -Seconds 2 }
}
if (-not $ok) { Write-Warning 'backend may not be ready (90s), check logs.' }
# ---------- 4) frontend ----------
Set-Location (Join-Path $root 'web')
Start-Process -FilePath 'npm' -ArgumentList 'run','dev'
Write-Host "`nAll started. backend http://127.0.0.1:8080  frontend http://127.0.0.1:5173" -ForegroundColor Green




