#!/usr/bin/env bash
# Run the Canteen Order Management System (Linux / macOS).
# Run ./compile.sh first. The DB must be up (see README.md).
set -e
cd "$(dirname "$0")"
java -cp "out:lib/mysql-connector-j.jar" com.canteen.ui.CanteenApp
