@echo off
setlocal enabledelayedexpansion

if not exist "%~dp0.env" (
    echo ERROR: No se encontro el archivo .env
    echo Copia .env.example a .env y completa las variables.
    pause
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0.env") do (
    set "_key=%%A"
    if not "!_key!"=="" if not "!_key:~0,1!"=="#" (
        set "%%A=%%B"
    )
)

cd /d %~dp0
mvnw.cmd spring-boot:run
