#!/bin/bash

# Run the HFT application with necessary JVM arguments for Chronicle Map compatibility on Java 17+
# These arguments allow access to internal APIs required for off-heap memory operations.

java \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -jar target/hft-0.0.1-SNAPSHOT.jar
