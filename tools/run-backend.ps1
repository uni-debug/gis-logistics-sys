param([string]$Workdir = 'D:\xiangmu2\01wuliupeisong\server', [switch]$Reset, [switch]$Repair)
$env:JAVA_HOME = 'E:\develop-java\jdk21'
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3306'
$env:DB_NAME = 'logistics_db'
$env:DB_USER = 'root'
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { Read-Host 'MySQL password (local dev only)' }
$env:PHONE_ENCRYPTION_KEY = 'dev-phone-key-32-chars!'
$env:OSRM_BASE_URL = 'http://127.0.0.1:5000'
Set-Location $Workdir
$cp = [System.IO.File]::ReadAllText('D:\xiangmu2\01wuliupeisong\server\cp.txt')
$fullCp = "target\classes;$cp"
$jwt = 'dev-secret-key-32-chars!'
$jwtArg = "-Dapp.jwt.secret=$jwt"
$flywayArgs = @('-Dspring.flyway.validate-on-migrate=false')
if ($Reset) {
  # 先停掉旧进程再重置，避免 Flyway RESET 时连接被占用
  Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
  Start-Sleep -Seconds 1
  $out = & "$env:JAVA_HOME\bin\java.exe" -cp $fullCp org.flywaydb.CommandLine -url "jdbc:mysql://127.0.0.1:3306/logistics_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" -user root -password $env:DB_PASSWORD reset 2>&1
  Write-Host "FLYWAY RESET: $($out -join ' ')"
  $out2 = & "$env:JAVA_HOME\bin\java.exe" -cp $fullCp org.flywaydb.CommandLine -url "jdbc:mysql://127.0.0.1:3306/logistics_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" -user root -password $env:DB_PASSWORD migrate 2>&1
  Write-Host "FLYWAY MIGRATE: $($out2 -join ' ')"
}
if (-not $Reset) { $flywayArgs = @() }
$args = @($jwtArg) + $flywayArgs + @('-cp', $fullCp, 'com.gis.logistics.LogisticsApplication')
if ($Repair) { $args = @('-Dspring.flyway.repair-on-migrate=true') + $args }
& "$env:JAVA_HOME\bin\java.exe" @args 2>&1 | Tee-Object -FilePath 'D:\xiangmu2\01wuliupeisong\tools\backend.log'
