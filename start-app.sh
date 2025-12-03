#!/bin/bash

# HFT Trading Application Startup Script
# Ensures Java 21 is used with all required JVM arguments

# Set Java 21
export JAVA_HOME=/Users/stackdev/Library/Java/JavaVirtualMachines/jdk-21.0.6.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# Verify Java version
echo "Using Java version:"
java -version

# Navigate to trading module
cd "$(dirname "$0")/hft-trading"

# Start application with Maven
echo ""
echo "Starting HFT Trading Application..."
mvn spring-boot:run
