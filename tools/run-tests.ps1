param([switch]$Offline, [string]$ArgLine = '')
Set-Location 'D:\xiangmu2\01wuliupeisong\server'
$env:JAVA_HOME = 'E:\develop-java\jdk21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
if ($Offline) {
  mvn -o -DskipTests=false test 2>&1 | Select-String -Pattern 'Tests run: 119','BUILD' | Select-Object -Last 4
} else {
  mvn -DskipTests=false test 2>&1 | Select-String -Pattern 'Tests run: 119','BUILD' | Select-Object -Last 4
}