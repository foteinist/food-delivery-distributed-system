@echo off
title Distributed Food Delivery System
color 0A
setlocal EnableDelayedExpansion

echo ==========================================
echo     DISTRIBUTED FOOD DELIVERY SYSTEM
echo ==========================================
echo.

REM ===== Check if gson exists =====
if not exist libs\gson-2.8.9.jar (
    echo ERROR: gson-2.8.9.jar not found in libs folder!
    echo Make sure the file exists at: libs\gson-2.8.9.jar
    echo.
    echo Download from: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
    pause
    exit /b 1
)

REM ===== Get local IPv4 (ignores virtual adapters) =====
for /f "tokens=14 delims=: " %%a in ('ipconfig ^| findstr /R /C:"IPv4.*192\."') do (
    set IP=%%a
)

if "%IP%"=="" (
    echo Could not detect local IP automatically.
    echo Make sure you are connected to a 192.x.x.x network.
    echo.
    set /p IP="Enter your local IP manually: "
)

echo Detected IP: %IP%
echo.

set /p USER_IP="Press Enter to use this IP or type another [%IP%]: "
if "%USER_IP%"=="" (
    set USER_IP=%IP%
)

echo Using IP: %USER_IP%
echo.
pause

:menu
cls
echo ==========================================
echo                MAIN MENU
echo ==========================================
echo 1. Compile Project
echo 2. Start Reducer
echo 3. Start Worker 1 (5001)
echo 4. Start Worker 2 (5002)
echo 5. Start Master
echo 6. Start Manager Console
echo 7. Start Client
echo 8. Start ALL (Reducer + 2 Workers + Master)
echo 9. Exit
echo ==========================================
echo.

set /p choice="Select option (1-9): "

if "%choice%"=="1" goto compile
if "%choice%"=="2" goto reducer
if "%choice%"=="3" goto worker1
if "%choice%"=="4" goto worker2
if "%choice%"=="5" goto master
if "%choice%"=="6" goto manager
if "%choice%"=="7" goto client
if "%choice%"=="8" goto all
if "%choice%"=="9" goto exit

echo Invalid option! Please try again.
timeout /t 2 >nul
goto menu

:compile
cls
echo ==========================================
echo            COMPILING PROJECT
echo ==========================================
echo.

echo Compiling BackEnd/*.java with Gson...
javac -cp ".;libs/gson-2.8.9.jar" -d . BackEnd\*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation FAILED!
    echo Check your Java files for errors.
    echo.
    pause
    goto menu
)

echo.
echo [SUCCESS] Compilation completed successfully!
echo.
pause
goto menu

:reducer
cls
echo Starting Reducer on %USER_IP%:5000...
start "Reducer - Port 5000" cmd /k java -cp ".;libs/gson-2.8.9.jar" Reducer %USER_IP% 5000
echo Reducer started in new window.
timeout /t 2 >nul
goto menu

:worker1
cls
echo Starting Worker 1 on port 5001...
start "Worker 1 - Port 5001" cmd /k java -cp ".;libs/gson-2.8.9.jar" Worker 5001
echo Worker 1 started in new window.
timeout /t 1 >nul
goto menu

:worker2
cls
echo Starting Worker 2 on port 5002...
start "Worker 2 - Port 5002" cmd /k java -cp ".;libs/gson-2.8.9.jar" Worker 5002
echo Worker 2 started in new window.
timeout /t 1 >nul
goto menu

:master
cls
echo Starting Master on %USER_IP%:5000 with 2 workers...
start "Master - Port 5000" cmd /k java -cp ".;libs/gson-2.8.9.jar" Master %USER_IP% 5000 2
echo Master started in new window.
timeout /t 2 >nul
goto menu

:manager
cls
echo Starting Manager Console connecting to %USER_IP%:5000...
start "Manager Console" cmd /k java -cp ".;libs/gson-2.8.9.jar" Manager %USER_IP% 5000
echo Manager started in new window.
timeout /t 1 >nul
goto menu

:client
cls
echo Starting Client connecting to %USER_IP%:5000...
start "Client" cmd /k java -cp ".;libs/gson-2.8.9.jar" Client %USER_IP% 5000
echo Client started in new window.
timeout /t 1 >nul
goto menu

:all
cls
echo ==========================================
echo         STARTING FULL SYSTEM
echo ==========================================
echo.
echo This will start:
echo - Reducer (port 5000)
echo - Worker 1 (port 5001)
echo - Worker 2 (port 5002)
echo - Master (port 5000)
echo.
echo IMPORTANT: Start order: Reducer → Workers → Master
echo.
pause

echo [1/4] Starting Reducer...
start "Reducer - Port 5000" cmd /k java -cp ".;libs/gson-2.8.9.jar" Reducer %USER_IP% 5000
timeout /t 3 >nul

echo [2/4] Starting Worker 1...
start "Worker 1 - Port 5001" cmd /k java -cp ".;libs/gson-2.8.9.jar" Worker 5001
timeout /t 2 >nul

echo [3/4] Starting Worker 2...
start "Worker 2 - Port 5002" cmd /k java -cp ".;libs/gson-2.8.9.jar" Worker 5002
timeout /t 2 >nul

echo [4/4] Starting Master...
start "Master - Port 5000" cmd /k java -cp ".;libs/gson-2.8.9.jar" Master %USER_IP% 5000 2
timeout /t 2 >nul

echo.
echo ==========================================
echo ALL COMPONENTS STARTED SUCCESSFULLY!
echo ==========================================
echo.
echo You can now start:
echo - Manager Console (option 6)
echo - Client (option 7)
echo.
pause
goto menu

:exit
cls
echo ==========================================
echo     Thank you for using the system!
echo ==========================================
timeout /t 2 >nul
exit
