@echo off
setlocal
cd /d "%~dp0"
echo Iniciando CRM Frontend...
call npm run dev
