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

$JAVAC_CMD -d bin -cp ".:lib/*:backend/src/main/resources" \
    backend/src/main/java/com/rentproof/database/*.java \
    backend/src/main/java/com/rentproof/landlord/*.java \
    backend/src/main/java/com/rentproof/property/*.java \
    backend/src/main/java/com/rentproof/payment/*.java \
    backend/src/main/java/com/rentproof/reminder/*.java \
    backend/src/main/java/com/rentproof/dashboard/*.java \
    backend/src/main/java/com/rentproof/*.java

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
