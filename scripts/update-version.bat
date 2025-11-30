@echo off
REM Script para Windows para actualizar la versión en Firestore
REM 
REM Uso:
REM   update-version.bat <versionCode> <versionName> [message]
REM
REM Ejemplo:
REM   update-version.bat 2 "1.1" "Nueva versión con mejoras"

if "%1"=="" (
    echo Error: Falta versionCode
    echo.
    echo Uso: update-version.bat ^<versionCode^> ^<versionName^> [message]
    echo Ejemplo: update-version.bat 2 "1.1" "Nueva versión con mejoras"
    exit /b 1
)

if "%2"=="" (
    echo Error: Falta versionName
    echo.
    echo Uso: update-version.bat ^<versionCode^> ^<versionName^> [message]
    echo Ejemplo: update-version.bat 2 "1.1" "Nueva versión con mejoras"
    exit /b 1
)

node scripts/update-version.js %1 %2 %3

