#!/usr/bin/env bash
# ============================================================
# RentProof - Compile and Run Verification Suites (macOS / Linux)
# ============================================================

set -e

mkdir -p bin

# Find Java compiler
JAVAC_CMD="javac"
JAVA_CMD="java"

if [ -x "$HOME/Library/Java/JavaVirtualMachines/jdk-25.0.5.1+2/Contents/Home/bin/javac" ]; then
    JAVAC_CMD="$HOME/Library/Java/JavaVirtualMachines/jdk-25.0.5.1+2/Contents/Home/bin/javac"
    JAVA_CMD="$HOME/Library/Java/JavaVirtualMachines/jdk-25.0.5.1+2/Contents/Home/bin/java"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC_CMD="$JAVA_HOME/bin/javac"
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

echo "============================================================"
echo "  Compiling RentProof Core Java & JDBC Backend"
echo "============================================================"

$JAVAC_CMD --release 25 -d bin -cp ".:lib/*:backend/src/main/resources" \
    backend/src/main/java/com/rentproof/database/*.java \
    backend/src/main/java/com/rentproof/landlord/*.java \
    backend/src/main/java/com/rentproof/property/*.java \
    backend/src/main/java/com/rentproof/tenant/*.java \
    backend/src/main/java/com/rentproof/payment/*.java \
    backend/src/main/java/com/rentproof/reminder/*.java \
    backend/src/main/java/com/rentproof/dashboard/*.java \
    backend/src/main/java/com/rentproof/auth/*.java \
    backend/src/main/java/com/rentproof/*.java

echo "[SUCCESS] Backend compiled successfully into bin/ folder!"
echo ""
echo "Running Shweta's Tenant Module Verification Suite..."
echo ""

$JAVA_CMD -ea -cp "bin:lib/*:backend/src/main/resources" com.rentproof.TenantDemo
