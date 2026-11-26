@echo off
echo ============================================================
echo   Starting RentProof Payment Management Web Server
echo ============================================================

if not exist bin mkdir bin

javac --release 25 -d bin -cp ".;lib/*;backend/src/main/resources" backend/src/main/java/com/rentproof/database/*.java backend/src/main/java/com/rentproof/landlord/*.java backend/src/main/java/com/rentproof/property/*.java backend/src/main/java/com/rentproof/payment/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting HTTP Server on http://localhost:8080 ...
start http://localhost:8080/frontend/payments/payments.html

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.payment.PaymentServer

pause

