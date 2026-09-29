$env:JAVA_HOME = 'E:\develop-java\jdk21'
$env:PATH = "E:\develop-java\jdk21\bin;" + $env:PATH
Set-Location D:\xiangmu2\01wuliupeisong\server
mvn -q -o spring-boot:run
