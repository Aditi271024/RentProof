#!/usr/bin/env bash
# ============================================================
# RentProof - Compile and Run Verification Suites (macOS / Linux)
# ============================================================

set -e

mkdir -p bin

# Find Java compiler
JAVAC_CMD="javac"
JAVA_CMD="java"

if [ -x "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/javac" ]; then
    JAVAC_CMD="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/javac"
    JAVA_CMD="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java"
fi

echo "============================================================"
echo "  Compiling RentProof Core Java & JDBC Backend"
echo "============================================================"

find backend/src/main/java -name "*.java" | sort > sources.txt

$JAVAC_CMD -d bin -cp ".:lib/*:backend/src/main/resources" @sources.txt

echo "[SUCCESS] Backend compiled successfully into bin/ folder!"
echo ""
echo "Running Gungun's Reminder Module Verification Suite..."
echo ""

$JAVA_CMD -cp "bin:lib/*:backend/src/main/resources" com.rentproof.ReminderDemo
