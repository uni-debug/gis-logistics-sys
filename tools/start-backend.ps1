Set-Location D:\xiangmu2\01wuliupeisong\server
$env:JAVA_HOME = 'E:\develop-java\jdk21'
$env:JWT_SECRET = 'dev-secret-key-32-chars!'
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { Read-Host 'MySQL password (local dev only)' }
$env:PHONE_ENCRYPTION_KEY = 'dev-phone-key-32chars!'
$java = 'E:\develop-java\jdk21\bin\java'
$jar = 'D:\xiangmu2\01wuliupeisong\server\target\logistics-server-0.1.0.jar'
$log = 'D:\xiangmu2\01wuliupeisong\tools\boot.log'
Remove-Item $log -ErrorAction SilentlyContinue
Remove-Item "$log.err" -ErrorAction SilentlyContinue
Start-Process -FilePath $java -ArgumentList @("-jar","$jar") -RedirectStandardOutput $log -RedirectStandardError "$log.err" -WindowStyle Hidden
Write-Host "java pid spawned, logging to $log"
