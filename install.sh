#!/bin/bash

# Create libs directory
mkdir -p libs

# Download NightCore jar
echo "Downloading NightCore..."
curl -L -o libs/nightcore-2.7.15.jar https://github.com/vanes430/nightcore-folia/releases/download/latest/nightcore-2.7.15.jar

# Install to local maven repository so the pom can find it
# This fulfills the requirement of making the pom use this jar
echo "Installing NightCore to local Maven repository..."
mvn install:install-file -Dfile=libs/nightcore-2.7.15.jar -DgroupId=su.nightexpress.nightcore -DartifactId=main -Dversion=2.7.15 -Dpackaging=jar

# Build the project
echo "Building project..."
mvn clean
mvn install
