#!/bin/bash

# usage: ./get_crash.sh com.example.DhoonHub
PACKAGE=$1

if [ -z "$PACKAGE" ]; then
  echo "❌ Please provide the package name. Example:"
  echo "   ./get_crash.sh com.example.DhoonHub"
  exit 1
fi

PID=$(adb shell pidof $PACKAGE)

if [ -z "$PID" ]; then
  echo "❌ App $PACKAGE is not running. Start the app first."
  exit 1
fi

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_FILE="crash_${PACKAGE}_${TIMESTAMP}.log"

echo "📋 Capturing crash logs for $PACKAGE (PID: $PID)..."
echo "Saving to $OUTPUT_FILE"
echo "Press CTRL+C to stop."

adb logcat --pid=$PID *:E | grep --line-buffered "FATAL EXCEPTION" -A 30 | tee $OUTPUT_FILE
