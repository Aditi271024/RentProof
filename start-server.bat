@echo off
echo ============================================================
echo   Starting RentProof Central Web & API Server
echo ============================================================

if not exist bin mkdir bin

if exist sources.txt del sources.txt
for /r backend\src\main\java %%F in (*.java) do echo "%%F" >> sources.txt
javac -d bin -cp ".;lib/*;backend/src/main/resources" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting HTTP Server on http://localhost:8080 ...
start http://localhost:8080/frontend/dashboard/dashboard.html

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofServer

pause
