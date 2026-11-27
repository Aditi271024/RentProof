
@echo off
echo ============================================================
echo Compiling RentProof Core Java ^& JDBC Backend
echo ============================================================

if not exist bin mkdir bin

if exist sources.txt del sources.txt

for /r backend\src\main\java %%F in (*.java) do echo "%%F" >> sources.txt

javac --release 17 -d bin -cp ".;lib/*;backend/src/main/resources" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo.
echo [SUCCESS] Backend compiled successfully!
echo.
echo Running RentProof Verification Suite...
echo.

java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.RentProofDemo

pause
