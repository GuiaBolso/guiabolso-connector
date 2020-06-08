#!/usr/bin/env bash

set -eo pipefail

DATADOG_VERSION=$(./gradlew properties --console=plain -q | grep "^datadog-version:" | awk '{print $2}')

echo "Build image..."
docker build -t guiabolso-connector:$VERSION \
       --build-arg VERSION=$VERSION \
       --build-arg DATADOG_VERSION=$DATADOG_VERSION \
       .
echo "Sign in on docker hub"
# Do signin
echo "Sign in successfully!"

echo "Publishing to docker hub"
#Do publish
echo "Published image guiabolso-connector:$VERSION successfully!"
