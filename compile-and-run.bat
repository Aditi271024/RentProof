@echo off
echo ============================================================
echo   Compiling RentProof Core Java & JDBC Backend
echo ============================================================

if not exist bin mkdir bin

javac -d bin -cp ".;lib/*;backend/src/main/resources" backend/src/main/java/com/rentproof/database/*.java backend/src/main/java/com/rentproof/landlord/*.java backend/src/main/java/com/rentproof/property/*.java backend/src/main/java/com/rentproof/payment/*.java backend/src/main/java/com/rentproof/reminder/*.java backend/src/main/java/com/rentproof/dashboard/*.java backend/src/main/java/com/rentproof/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please check Java errors above.
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Backend compiled successfully into /bin folder!
echo.
echo Running RentProof Verification Suite...
echo.

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofDemo

pause
