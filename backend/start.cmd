@echo off
setlocal
cd /d "%~dp0"
if not defined SPRING_PROFILES_ACTIVE set SPRING_PROFILES_ACTIVE=local
echo Iniciando CRM Backend...
call mvn spring-boot:run
