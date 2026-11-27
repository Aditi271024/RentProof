@echo off
echo ============================================================
echo   Starting RentProof Central Web & API Server
echo ============================================================

if not exist bin mkdir bin

javac -d bin -cp ".;lib/*;backend/src/main/resources" backend/src/main/java/com/rentproof/database/*.java backend/src/main/java/com/rentproof/landlord/*.java backend/src/main/java/com/rentproof/property/*.java backend/src/main/java/com/rentproof/payment/*.java backend/src/main/java/com/rentproof/reminder/*.java backend/src/main/java/com/rentproof/dashboard/*.java backend/src/main/java/com/rentproof/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting HTTP Server on http://localhost:8080 ...
start http://localhost:8080/frontend/dashboard/dashboard.html

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofServer

pause
