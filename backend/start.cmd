@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Usando Java:
java -version
echo.
echo Iniciando CRM Backend...
mvn spring-boot:run
