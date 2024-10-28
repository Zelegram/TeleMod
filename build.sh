#!/bin/bash
set -e  # Exit on any command failure

# Function to build Standalone APK
standalone() {
  echo "Building Standalone APKs..."
  start_time=$(date +%s)
  ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandalone && \
    echo "APKs saved in: TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone"
  end_time=$(date +%s)
  echo "Standalone build completed in $((end_time - start_time)) seconds."
}

# Function to build Play Store APK
release() {
  echo "Building App (Play Store Version)..."
  start_time=$(date +%s)
  ./gradlew :TMessagesProj_App:assembleAfatRelease && \
    echo "APKs saved in: TMessagesProj_App/build/outputs/apk/afat/release"
  end_time=$(date +%s)
  echo "App build completed in $((end_time - start_time)) seconds."
}

# Function to build Huawei APK
huawei() {
  echo "Building Huawei APKs..."
  start_time=$(date +%s)
  ./gradlew :TMessagesProj_AppHuawei:assembleAfatRelease && \
    echo "APKs saved in: TMessagesProj_AppHuawei/build/outputs/apk/afat/release"
  end_time=$(date +%s)
  echo "Huawei build completed in $((end_time - start_time)) seconds."
}

# Function to build all APKs if no arguments are provided
build_all() {
  echo "No arguments provided. Running all builds..."
  standalone
  release
  huawei
}

# Help message for usage
usage() {
  echo "Usage: $0 [standalone|release|huawei]"
  echo "  standalone   Build Standalone APKs"
  echo "  release          Build Play Store APKs"
  echo "  huawei       Build Huawei APKs"
  echo "  no argument  Build all APKs"
  exit 1
}

# Check if any arguments are provided
if [ $# -eq 0 ]; then
  build_all
else
  # Execute based on the argument
  case "$1" in
    standalone) standalone ;;
    release) release ;;
    huawei) huawei ;;
    *) usage ;;  # Show help message for invalid arguments
  esac
fi
