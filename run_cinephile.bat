@echo off
title Cinephile - Cinema Ticket Booking System
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "db.properties" (
    if exist "db.properties.example" (
        copy /Y "db.properties.example" "db.properties" >nul
        echo [SETUP] Created db.properties from template.
        echo         Edit db.properties with your MySQL root password before first use.
    )
)

where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot\bin\javac.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\javac.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    )
    if defined JAVA_HOME (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    )
)

for /f "delims=" %%I in ('where javac 2^>nul') do (
    set "JDK_BIN=%%~dpI"
    goto :found_jdk
)
:found_jdk
if defined JDK_BIN (
    set "PATH=%JDK_BIN%;%PATH%"
) else (
    echo [ERROR] Java JDK ^(javac^) was not found on PATH.
    echo         Please install JDK 8 or newer and try again.
    pause
    exit /b 1
)

echo Compiling Cinephile Java sources...
if exist "out" rmdir /s /q out
mkdir out
dir /s /B src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.3.0.jar" -d out @sources.txt
del sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo Launching Cinephile Web Server on http://localhost:8080 ...
java -cp "out;lib\mysql-connector-j-8.3.0.jar" com.cinephile.web.CinephileWebServer
pause
