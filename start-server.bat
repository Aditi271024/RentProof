@echo off
echo ============================================================
echo   Starting RentProof Unified Application Server (Port 8080)
echo   Modules: Landlord, Property, Room, Tenant, Payment, Reminder, Dashboard
echo ============================================================

if not exist bin mkdir bin

if exist sources.txt del sources.txt
for /r backend\src\main\java %%F in (*.java) do echo "%%F" >> sources.txt

echo Compiling Java backend (Target: Java 17 LTS)...
javac --release 17 -d bin -cp ".;lib/*;backend/src/main/resources" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. Please check the Java errors above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] Backend compiled successfully into bin/ folder.
echo.
echo ============================================================
echo   RentProof Server is starting up
echo   Open in browser: http://localhost:8080/frontend/auth/login.html
echo   Press Ctrl+C in this window to stop the server.
echo ============================================================
echo.

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofServer

pause
