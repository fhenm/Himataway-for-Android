#!/usr/bin/env bash

echo "Upload to DeployGate"

curl \
    -F "token=ad2141e095d70b00f12ed1ceeb25e6e55a42d26a" \
    -F "file=@$MOBILECENTER_OUTPUT_DIRECTORY/himataway-release.apk" \
    -F "message=build by Visual Studio Mobile Center - #$MOBILECENTER_BUILD_ID" \
    https://deploygate.com/api/users/amay077/apps
