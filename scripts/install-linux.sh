#!/bin/bash

echo "========================================"
echo "AWS Secrets Manager APIM SDK - Linux Installer"
echo "========================================"
echo

# Check if Axway is installed
if [ -z "$AXWAY_HOME" ]; then
    echo "ERROR: AXWAY_HOME environment variable not set"
    echo
    echo "Please set the AXWAY_HOME environment variable"
    echo "Example: export AXWAY_HOME=/opt/Axway/API_Gateway/7.7.0.20240830"
    echo
    exit 1
fi

if [ ! -d "$AXWAY_HOME" ]; then
    echo "ERROR: Axway directory not found: $AXWAY_HOME"
    echo
    echo "Check if the path is correct and Axway is installed"
    echo
    exit 1
fi

echo "Axway found at: $AXWAY_HOME"
echo

# Check if main JAR exists
MAIN_JAR=$(find . -name "aws-secretsmanager-apim-sdk-*.jar" | head -1)
if [ -z "$MAIN_JAR" ]; then
    echo "ERROR: Main JAR not found"
    echo
    echo "Make sure the file aws-secretsmanager-apim-sdk-*.jar is present"
    echo
    exit 1
fi

echo "Main JAR found: $MAIN_JAR"
echo

# Create backup of lib directory
BACKUP_DIR="lib_backup_$(date +%Y%m%d_%H%M%S)"
echo "Creating backup at: $AXWAY_HOME/ext/lib/$BACKUP_DIR"
mkdir -p "$AXWAY_HOME/ext/lib"
if [ -d "$AXWAY_HOME/ext/lib" ]; then
    cp -r "$AXWAY_HOME/ext/lib" "$AXWAY_HOME/ext/lib/$BACKUP_DIR" 2>/dev/null
    echo "Backup created successfully"
else
    echo "ext/lib directory does not exist, it will be created"
fi
echo

# Copy main JAR
echo "Copying main JAR..."
cp "$MAIN_JAR" "$AXWAY_HOME/ext/lib/"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to copy main JAR"
    exit 1
fi
echo "Main JAR copied successfully"
echo

# Copy dependencies if they exist
if [ -d "dependencies" ]; then
  echo "Copying dependencies..."
  mkdir -p "$AXWAY_HOME/ext/lib/dependencies"
  cp dependencies/* "$AXWAY_HOME/ext/lib/dependencies/" 2>/dev/null
  if [ $? -eq 0 ]; then
    echo "Dependencies copied successfully"
  else
    echo "WARNING: Some dependencies could not be copied"
  fi
  echo
else
  echo "No dependencies found to copy"
  echo
fi

# Copy Policy Studio resources if they exist
if [ -d "resources" ]; then
  echo "Copying Policy Studio resources..."
  if [ -d "resources/fed" ]; then
    mkdir -p "$AXWAY_HOME/ext/lib/fed"
    cp resources/fed/* "$AXWAY_HOME/ext/lib/fed/" 2>/dev/null
    echo "FED resources copied successfully"
  fi
  if [ -d "resources/yaml" ]; then
    mkdir -p "$AXWAY_HOME/ext/lib/yaml"
    cp resources/yaml/* "$AXWAY_HOME/ext/lib/yaml/" 2>/dev/null
    echo "YAML resources copied successfully"
  fi
  echo
else
  echo "No resources found to copy"
  echo
fi

# Check if Policy Studio is running
echo "Checking if Policy Studio is running..."
if pgrep -f "policystudio" > /dev/null; then
    echo "WARNING: Policy Studio is running"
    echo "It is recommended to close Policy Studio before continuing"
    echo
    read -p "Do you want to continue anyway? (Y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation cancelled"
        exit 0
    fi
    echo
fi

# Check if API Gateway is running
echo "Checking if API Gateway is running..."
if pgrep -f "apigateway" > /dev/null; then
    echo "WARNING: API Gateway is running"
    echo "It is recommended to stop the service before continuing"
    echo
    read -p "Do you want to continue anyway? (Y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation cancelled"
        exit 0
    fi
    echo
fi

echo "========================================"
echo "Installation completed successfully!"
echo "========================================"
echo
echo "Installed files:"
echo "- $MAIN_JAR -> $AXWAY_HOME/ext/lib/"
if [ -d "dependencies" ]; then
    echo "- Dependencies -> $AXWAY_HOME/ext/lib/dependencies/"
fi
echo
echo "Backup created at: $AXWAY_HOME/ext/lib/$BACKUP_DIR"
echo
echo "Next steps:"
echo "1. Restart Policy Studio"
echo "2. Restart API Gateway"
echo "3. The AWS Lambda filter will be available in Policy Studio"
echo
echo "To uninstall, restore the backup or delete the copied files"
echo 