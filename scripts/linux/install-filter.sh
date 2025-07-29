#!/bin/bash

# AWS Secrets Manager Filter installation script for Axway API Gateway (Linux)
# Author: Assistant
# Date: $(date)
# Note: YAML files are organized in src/main/resources/yaml/
# For Windows, use: install-filter-windows.ps1 or install-filter-windows.cmd

AXWAY_DIR="/opt/axway/Axway-7.7.0.20240830"
# Get the JAR file name dynamically
JAR_FILE=$(find build/libs -name "aws-secretsmanager-apim-sdk-*.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR file not found. Please run './gradlew build' first."
    exit 1
fi

EXT_LIB_DIR="$AXWAY_DIR/apigateway/groups/group-2/instance-1/ext/lib"

echo "========================================"
echo "AWS Secrets Manager APIM SDK - Linux Installer"
echo "========================================"
echo

echo "Axway directory: $AXWAY_DIR"
echo "JAR: $JAR_FILE"
echo ""

# Check if the Axway directory exists
if [ ! -d "$AXWAY_DIR" ]; then
    echo "❌ Error: Axway directory not found: $AXWAY_DIR"
    exit 1
fi

# Create ext/lib directory if it does not exist
if [ ! -d "$EXT_LIB_DIR" ]; then
    echo "📁 Creating directory: $EXT_LIB_DIR"
    mkdir -p "$EXT_LIB_DIR"
fi

# Copy JAR to ext/lib directory
echo "📦 Copying JAR to: $EXT_LIB_DIR"
cp "$JAR_FILE" "$EXT_LIB_DIR/"

# Check if the copy was successful
if [ $? -eq 0 ]; then
    echo "✅ JAR copied successfully"
else
    echo "❌ Error copying JAR"
    exit 1
fi

# List JARs in the directory
echo ""
echo "📋 JARs in ext/lib directory:"
ls -la "$EXT_LIB_DIR"/*.jar

echo ""
echo "=== Installation Completed ==="
echo ""
echo "✅ Installation completed successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Restart the Axway API Gateway"
echo "2. Open Policy Studio"
echo "3. Add the JAR: $EXT_LIB_DIR/$(basename "$JAR_FILE")"
echo "4. Restart Policy Studio with -clean"
echo "5. Search for 'AWS Secrets Manager Filter' in the palette" 