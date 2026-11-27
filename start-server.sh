#!/usr/bin/env bash
# ============================================================
# RentProof - Start Central Web & API Server (port 8080)
# ============================================================

set -e

mkdir -p bin

JAVAC_CMD="javac"
JAVA_CMD="java"

if [ -x "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/javac" ]; then
    JAVAC_CMD="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/javac"
    JAVA_CMD="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java"
fi

echo "============================================================"
echo "  Compiling RentProof Core Java & Server Backend"
echo "============================================================"

find backend/src/main/java -name "*.java" | sort > sources.txt

$JAVAC_CMD -d bin -cp ".:lib/*:backend/src/main/resources" @sources.txt

echo "[SUCCESS] Backend compiled successfully into bin/!"
echo ""
echo "============================================================"
echo "  Starting RentProof HTTP Server on http://localhost:8080"
echo "============================================================"
echo "Dashboard: http://localhost:8080/frontend/dashboard/dashboard.html"
echo "Reminders: http://localhost:8080/frontend/reminders/reminders.html"
echo "Payments:  http://localhost:8080/frontend/payments/payments.html"
echo "Properties:http://localhost:8080/frontend/properties/properties.html"
echo "============================================================"
echo ""

$JAVA_CMD -cp "bin:lib/*:backend/src/main/resources" com.rentproof.RentProofServer
