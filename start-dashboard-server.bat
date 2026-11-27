@echo off
echo ============================================================
echo   Starting RentProof Landlord Dashboard Server
echo ============================================================

if not exist bin mkdir bin

javac -d bin -cp ".;lib/*;backend/src/main/resources" backend/src/main/java/com/rentproof/database/*.java backend/src/main/java/com/rentproof/landlord/*.java backend/src/main/java/com/rentproof/property/*.java backend/src/main/java/com/rentproof/dashboard/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Backend compiled successfully.
echo Starting HTTP server on http://localhost:8080/frontend/dashboard/dashboard.html
echo Press Ctrl+C to stop the server.
echo.

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.dashboard.DashboardServer

pause
